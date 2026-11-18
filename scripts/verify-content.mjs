#!/usr/bin/env node
/**
 * verify-content: the content build check.
 *
 *  1. Schema-validates every series.yaml, chapter.yaml, teasers.yaml and questions.yaml.
 *  2. Runs every code/*.java with the JDK and compares stdout to its .out file, byte for byte (line endings aside).
 *  3. Runs every `predict` question's code and checks it prints `expected` (same normalising rule as the grader).
 *  4. Checks that theory.mdx only references code files listed in chapter.yaml.
 *
 * Usage:  node scripts/verify-content.mjs [--no-run] [--write-out]
 *   --no-run     schema checks only (no JDK needed)
 *   --write-out  regenerate .out files from real runs (then review the diff!)
 *
 * Needs js-yaml, resolved from apps/web/node_modules (run `npm ci` in apps/web once).
 */
import { createRequire } from 'node:module';
import { spawn } from 'node:child_process';
import { mkdtemp, readFile, readdir, rm, stat, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const CONTENT = path.join(ROOT, 'content');
const args = new Set(process.argv.slice(2));
const RUN = !args.has('--no-run');
const WRITE_OUT = args.has('--write-out');

const require = createRequire(path.join(ROOT, 'apps/web/package.json'));
let yaml;
try {
  yaml = require('js-yaml');
} catch {
  console.error('js-yaml not found. Run `npm ci` in apps/web first.');
  process.exit(2);
}

const errors = [];
const fail = (file, msg) => errors.push(`${path.relative(ROOT, file)}: ${msg}`);
let checks = 0;

// ---------- tiny schema helpers ----------
const isStr = (v) => typeof v === 'string' && v.trim().length > 0;
const isInt = (v) => Number.isInteger(v);
const isStrList = (v) => Array.isArray(v) && v.every(isStr);

function need(file, obj, field, pred, what) {
  checks++;
  if (!pred(obj?.[field])) fail(file, `${obj?.id ? `${obj.id}: ` : ''}'${field}' must be ${what}`);
}
function optional(file, obj, field, pred, what) {
  if (obj?.[field] !== undefined && obj?.[field] !== null) need(file, obj, field, pred, what);
}
function uniqueIds(file, items, kind) {
  const seen = new Set();
  for (const it of items) {
    checks++;
    if (seen.has(it.id)) fail(file, `duplicate ${kind} id '${it.id}'`);
    seen.add(it.id);
  }
}

async function loadYaml(file) {
  try {
    return yaml.load(await readFile(file, 'utf8'));
  } catch (e) {
    fail(file, `YAML parse error: ${e.message}`);
    return null;
  }
}

/** Same rule as PredictOutputGrader: unify line endings, strip trailing spaces per line, strip blank edges. */
const normalise = (s) => s.replace(/\r\n?/g, '\n').split('\n').map((l) => l.replace(/\s+$/, '')).join('\n').trim();

// ---------- schema per file ----------
const TIERS = ['warmup', 'interview', 'killer'];
const TYPES = ['mcq', 'predict', 'short', 'problem'];

function checkQuestion(file, q) {
  need(file, q, 'id', isStr, 'a non-empty string');
  need(file, q, 'tier', (v) => TIERS.includes(v), `one of ${TIERS}`);
  need(file, q, 'type', (v) => TYPES.includes(v), `one of ${TYPES}`);
  optional(file, q, 'xp', isInt, 'an integer');
  switch (q.type) {
    case 'mcq':
      need(file, q, 'prompt', isStr, 'a non-empty string');
      need(file, q, 'options', (v) => isStrList(v) && v.length >= 2, 'a list of at least 2 strings');
      need(file, q, 'answer', (v) => isInt(v) && v >= 0 && v < (q.options?.length ?? 0), 'a valid option index');
      need(file, q, 'explanation', isStr, 'a non-empty string');
      break;
    case 'predict':
      need(file, q, 'code', (v) => isStr(v) && /public\s+class\s+\w+/.test(v), 'a full program with a public class');
      need(file, q, 'expected', isStr, 'the exact output');
      need(file, q, 'explanation', isStr, 'a non-empty string');
      break;
    case 'short':
      need(file, q, 'prompt', isStr, 'a non-empty string');
      need(file, q, 'modelAnswer', isStr, 'a non-empty string');
      need(file, q, 'keyPoints', (v) => isStrList(v) && v.length > 0, 'a non-empty list of strings');
      break;
    case 'problem':
      need(file, q, 'prompt', isStr, 'a non-empty string');
      need(file, q, 'hints', (v) => isStrList(v) && v.length === 3, 'exactly 3 hints');
      need(file, q, 'modelAnswer', isStr, 'a non-empty string');
      break;
  }
}

async function checkChapter(dir, predictJobs, outJobs) {
  const chapterFile = path.join(dir, 'chapter.yaml');
  if (!existsSync(chapterFile)) return fail(dir, 'missing chapter.yaml');
  const ch = await loadYaml(chapterFile);
  if (!ch) return;
  need(chapterFile, ch, 'id', (v) => isStr(v) && /^[a-z0-9-]+(\.[a-z0-9-]+)*$/.test(v), 'a dotted lowercase id');
  need(chapterFile, ch, 'title', isStr, 'a non-empty string');
  need(chapterFile, ch, 'lesson', isInt, 'an integer');
  optional(chapterFile, ch, 'whereAsked', isStrList, 'a list of strings');
  const codeFiles = ch.codeFiles ?? [];
  need(chapterFile, ch, 'codeFiles', (v) => v === undefined || isStrList(v), 'a list of file names');

  for (const f of codeFiles) {
    const src = path.join(dir, 'code', f);
    const out = src.replace(/\.java$/, '.out');
    checks++;
    if (!existsSync(src)) fail(chapterFile, `code file ${f} not found`);
    else if (!existsSync(out) && !WRITE_OUT) fail(chapterFile, `${f} has no verified output file ${path.basename(out)}`);
    else outJobs.push({ src, out });
  }

  const theory = path.join(dir, 'theory.mdx');
  checks++;
  if (!existsSync(theory)) fail(dir, 'missing theory.mdx');
  else {
    const mdx = await readFile(theory, 'utf8');
    for (const m of mdx.matchAll(/<RunnableCode[^>]*file="([^"]+)"/g)) {
      checks++;
      if (!codeFiles.includes(m[1])) fail(theory, `<RunnableCode file="${m[1]}"> is not listed in chapter.yaml codeFiles`);
    }
  }

  const teasersFile = path.join(dir, 'teasers.yaml');
  if (existsSync(teasersFile)) {
    const t = await loadYaml(teasersFile);
    const teasers = t?.teasers ?? [];
    uniqueIds(teasersFile, teasers, 'teaser');
    for (const tz of teasers) {
      need(teasersFile, tz, 'id', isStr, 'a non-empty string');
      need(teasersFile, tz, 'prompt', isStr, 'a non-empty string');
      need(teasersFile, tz, 'options', (v) => isStrList(v) && v.length >= 2, 'a list of at least 2 strings');
      need(teasersFile, tz, 'answer', (v) => isInt(v) && v >= 0 && v < (tz.options?.length ?? 0), 'a valid option index');
      need(teasersFile, tz, 'reveal', isStr, 'a non-empty string');
    }
  }

  const questionsFile = path.join(dir, 'questions.yaml');
  if (existsSync(questionsFile)) {
    const q = await loadYaml(questionsFile);
    const questions = q?.questions ?? [];
    uniqueIds(questionsFile, questions, 'question');
    for (const question of questions) {
      checkQuestion(questionsFile, question);
      if (question.type === 'predict' && isStr(question.code)) predictJobs.push({ file: questionsFile, q: question });
    }
    const cards = q?.flashcards ?? [];
    uniqueIds(questionsFile, cards, 'flashcard');
    for (const c of cards) {
      need(questionsFile, c, 'front', isStr, 'a non-empty string');
      need(questionsFile, c, 'back', isStr, 'a non-empty string');
    }
  }
  return ch.id;
}

// ---------- running Java ----------
function runJava(file, cwd) {
  return new Promise((resolve) => {
    const child = spawn('java', ['-Xmx128m', file], { cwd, stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    const timer = setTimeout(() => child.kill('SIGKILL'), 30_000);
    child.stdout.on('data', (d) => (stdout += d));
    child.stderr.on('data', (d) => (stderr += d));
    child.on('error', (e) => resolve({ code: -1, stdout, stderr: String(e) }));
    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({ code, stdout, stderr });
    });
  });
}

async function runPredict({ file, q }) {
  const dir = await mkdtemp(path.join(tmpdir(), 'sc-verify-'));
  try {
    const cls = q.code.match(/public\s+class\s+(\w+)/)[1];
    await writeFile(path.join(dir, `${cls}.java`), q.code);
    const r = await runJava(`${cls}.java`, dir);
    checks++;
    if (r.code !== 0) fail(file, `${q.id}: program failed (exit ${r.code}): ${r.stderr.trim().split('\n')[0]}`);
    else if (normalise(r.stdout) !== normalise(q.expected)) {
      fail(file, `${q.id}: expected does not match the real output.\n--- expected\n${normalise(q.expected)}\n--- actual\n${normalise(r.stdout)}`);
    }
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
}

async function runCodeFile({ src, out }) {
  const r = await runJava(path.basename(src), path.dirname(src));
  checks++;
  if (r.code !== 0) return fail(src, `program failed (exit ${r.code}): ${r.stderr.trim().split('\n')[0]}`);
  const actual = r.stdout.replace(/\r\n/g, '\n');
  if (WRITE_OUT) {
    await writeFile(out, actual);
    return;
  }
  const stored = (await readFile(out, 'utf8')).replace(/\r\n/g, '\n');
  if (stored !== actual) fail(out, 'stored output differs from a fresh run (re-run with --write-out and review the diff)');
}

async function pool(jobs, size, fn) {
  const queue = [...jobs];
  await Promise.all(Array.from({ length: Math.min(size, queue.length) }, async () => {
    while (queue.length) await fn(queue.shift());
  }));
}

// ---------- main ----------
const seriesDirs = (await readdir(CONTENT, { withFileTypes: true })).filter((d) => d.isDirectory());
const predictJobs = [];
const outJobs = [];
const chapterIds = new Set();
let chapterCount = 0;

for (const d of seriesDirs) {
  const seriesFile = path.join(CONTENT, d.name, 'series.yaml');
  if (!existsSync(seriesFile)) continue;
  const s = await loadYaml(seriesFile);
  if (!s) continue;
  need(seriesFile, s, 'id', isStr, 'a non-empty string');
  need(seriesFile, s, 'title', isStr, 'a non-empty string');
  need(seriesFile, s, 'units', (v) => Array.isArray(v) && v.length > 0, 'a non-empty list');
  for (const u of s.units ?? []) {
    need(seriesFile, u, 'id', isStr, 'a non-empty string');
    need(seriesFile, u, 'dir', isStr, 'a folder name');
    need(seriesFile, u, 'chapters', isStrList, 'a list of chapter folders');
    for (const folder of u.chapters ?? []) {
      const dir = path.join(CONTENT, d.name, u.dir ?? '', folder);
      checks++;
      if (!existsSync(dir) || !(await stat(dir)).isDirectory()) {
        fail(seriesFile, `unit ${u.id} lists '${folder}' but ${path.relative(ROOT, dir)} does not exist`);
        continue;
      }
      const id = await checkChapter(dir, predictJobs, outJobs);
      chapterCount++;
      if (id) {
        checks++;
        if (chapterIds.has(id)) fail(dir, `duplicate chapter id ${id}`);
        chapterIds.add(id);
      }
    }
  }
}

if (RUN) {
  console.log(`Running ${outJobs.length} code file(s) and ${predictJobs.length} predict question(s) on the JDK...`);
  await pool(outJobs, 4, runCodeFile);
  await pool(predictJobs, 4, runPredict);
}

if (errors.length) {
  console.error(`\nverify-content FAILED (${errors.length} problem${errors.length > 1 ? 's' : ''}):\n`);
  for (const e of errors) console.error(` - ${e}`);
  process.exit(1);
}
console.log(`verify-content OK: ${chapterCount} chapter(s), ${checks} checks${RUN ? ', every snippet re-run and matched' : ' (schema only)'}${WRITE_OUT ? ', .out files rewritten' : ''}.`);

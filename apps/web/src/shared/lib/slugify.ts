/** Stable, URL-safe id for a heading: "How a HashSet picks the bucket (exact internals)" -> "how-a-hashset-picks-the-bucket-exact-internals". */
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80);
}

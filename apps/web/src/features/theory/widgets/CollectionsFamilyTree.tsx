import { HierarchyTree, type TreeNode } from '@/shared/kit';

const collectionRoot: TreeNode = {
  label: 'Iterable',
  kind: 'interface',
  family: 'root',
  note: 'Anything you can for-each over. It only promises iterator().',
  children: [
    {
      label: 'Collection',
      kind: 'interface',
      family: 'root',
      note: 'A group of elements: add, remove, contains, size, stream.',
      children: [
        {
          label: 'List',
          kind: 'interface',
          family: 'list',
          note: 'Ordered, duplicates allowed, index access get(i).',
          children: [
            { label: 'ArrayList', kind: 'class', family: 'list', note: 'Backed by a resizable array. O(1) get(i), amortised O(1) add at the end.' },
            { label: 'LinkedList', kind: 'class', family: 'list', note: 'Doubly linked nodes. Also a Deque. O(n) get(i): rarely the right choice.' },
          ],
        },
        {
          label: 'Set',
          kind: 'interface',
          family: 'set',
          note: 'No duplicates, decided by equals (and hashCode for Hash* sets).',
          children: [
            {
              label: 'HashSet',
              kind: 'class',
              family: 'set',
              note: 'A HashMap with dummy values. O(1) average, no order guarantee.',
              children: [{ label: 'LinkedHashSet', kind: 'class', family: 'set', note: 'HashSet + a linked list through entries: insertion order.' }],
            },
            {
              label: 'SortedSet',
              kind: 'interface',
              family: 'set',
              note: 'Kept sorted. NavigableSet adds floor, ceiling, headSet...',
              children: [{ label: 'TreeSet', kind: 'class', family: 'set', note: 'Red-black tree (a TreeMap inside). O(log n), sorted.' }],
            },
          ],
        },
        {
          label: 'Queue',
          kind: 'interface',
          family: 'queue',
          note: 'Hold elements for processing: offer, poll, peek.',
          children: [
            {
              label: 'Deque',
              kind: 'interface',
              family: 'queue',
              note: 'Double-ended: works as a queue (FIFO) or a stack (LIFO).',
              children: [{ label: 'ArrayDeque', kind: 'class', family: 'queue', note: 'Circular array. The default stack AND queue in modern Java.' }],
            },
            { label: 'PriorityQueue', kind: 'class', family: 'queue', note: 'Binary heap. Head is always the smallest. O(log n) offer/poll.' },
          ],
        },
      ],
    },
  ],
};

const mapRoot: TreeNode = {
  label: 'Map',
  kind: 'interface',
  family: 'map',
  note: 'Key-to-value pairs. NOT a Collection: there is no single element type.',
  children: [
    {
      label: 'HashMap',
      kind: 'class',
      family: 'map',
      note: 'Array of buckets. O(1) average get/put, no order.',
      children: [{ label: 'LinkedHashMap', kind: 'class', family: 'map', note: 'Insertion or access order. accessOrder=true + removeEldestEntry = LRU cache.' }],
    },
    {
      label: 'SortedMap',
      kind: 'interface',
      family: 'map',
      note: 'Keys kept sorted. NavigableMap adds floorKey, ceilingKey, subMap...',
      children: [{ label: 'TreeMap', kind: 'class', family: 'map', note: 'Red-black tree. O(log n), sorted keys, range queries.' }],
    },
  ],
};

export function CollectionsFamilyTree() {
  return <HierarchyTree roots={[collectionRoot, mapRoot]} caption="Two roots. Dashed = interface (a contract), solid = class (you can new it). Colour = family." />;
}

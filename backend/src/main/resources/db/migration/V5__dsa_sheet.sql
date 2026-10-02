-- =====================================================================
-- StudyVault - FAANG DSA 150 sheet (interactive, trackable)
-- 150 curated questions across 16 topics. Progress tracking per user.
-- =====================================================================

CREATE TABLE dsa_topics (
    id         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(80)  NOT NULL UNIQUE,
    slug       VARCHAR(80)  NOT NULL UNIQUE,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE dsa_questions (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic_id     INT         NOT NULL REFERENCES dsa_topics(id) ON DELETE CASCADE,
    position     INT         NOT NULL,
    title        VARCHAR(200) NOT NULL,
    slug         VARCHAR(200) NOT NULL UNIQUE,
    difficulty   VARCHAR(10) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    leetcode_url VARCHAR(300),
    CONSTRAINT uq_dsa_question_position UNIQUE (topic_id, position)
);
CREATE INDEX idx_dsa_questions_topic ON dsa_questions (topic_id, position);

CREATE TABLE dsa_progress (
    user_id     UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id INT    NOT NULL REFERENCES dsa_questions(id) ON DELETE CASCADE,
    done        BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, question_id)
);
CREATE INDEX idx_dsa_progress_user ON dsa_progress (user_id) WHERE done;

INSERT INTO dsa_topics (name, slug, sort_order) VALUES
  ('Arrays', 'arrays', 1),
  ('Strings', 'strings', 2),
  ('Two Pointers', 'two-pointers', 3),
  ('Sliding Window', 'sliding-window', 4),
  ('Binary Search', 'binary-search', 5),
  ('Linked List', 'linked-list', 6),
  ('Stacks & Queues', 'stacks-queues', 7),
  ('Trees', 'trees', 8),
  ('Heaps', 'heaps', 9),
  ('Backtracking', 'backtracking', 10),
  ('Tries', 'tries', 11),
  ('Graphs', 'graphs', 12),
  ('Greedy', 'greedy', 13),
  ('Intervals', 'intervals', 14),
  ('1-D Dynamic Programming', 'dp-1d', 15),
  ('2-D Dynamic Programming', 'dp-2d', 16),
  ('Bit Manipulation', 'bit-manipulation', 17);

-- Arrays (17)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 1,  'Two Sum', 'two-sum', 'EASY', 'https://leetcode.com/problems/two-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 2,  'Best Time to Buy and Sell Stock', 'best-time-to-buy-and-sell-stock', 'EASY', 'https://leetcode.com/problems/best-time-to-buy-and-sell-stock/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 3,  'Contains Duplicate', 'contains-duplicate', 'EASY', 'https://leetcode.com/problems/contains-duplicate/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 4,  'Missing Number', 'missing-number', 'EASY', 'https://leetcode.com/problems/missing-number/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 5,  'Majority Element', 'majority-element', 'EASY', 'https://leetcode.com/problems/majority-element/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 7,  'Product of Array Except Self', 'product-of-array-except-self', 'MEDIUM', 'https://leetcode.com/problems/product-of-array-except-self/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 8,  'Maximum Subarray', 'maximum-subarray', 'MEDIUM', 'https://leetcode.com/problems/maximum-subarray/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 9,  '3Sum', '3sum', 'MEDIUM', 'https://leetcode.com/problems/3sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 10, 'Container With Most Water', 'container-with-most-water', 'MEDIUM', 'https://leetcode.com/problems/container-with-most-water/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 11, 'Next Permutation', 'next-permutation', 'MEDIUM', 'https://leetcode.com/problems/next-permutation/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 12, 'Rotate Array', 'rotate-array', 'MEDIUM', 'https://leetcode.com/problems/rotate-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 13, 'Sort Colors', 'sort-colors', 'MEDIUM', 'https://leetcode.com/problems/sort-colors/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 14, 'Set Matrix Zeroes', 'set-matrix-zeroes', 'MEDIUM', 'https://leetcode.com/problems/set-matrix-zeroes/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 15, 'Spiral Matrix', 'spiral-matrix', 'MEDIUM', 'https://leetcode.com/problems/spiral-matrix/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 16, 'Subarray Sum Equals K', 'subarray-sum-equals-k', 'MEDIUM', 'https://leetcode.com/problems/subarray-sum-equals-k/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 17, 'Longest Consecutive Sequence', 'longest-consecutive-sequence', 'MEDIUM', 'https://leetcode.com/problems/longest-consecutive-sequence/'),
  ((SELECT id FROM dsa_topics WHERE slug='arrays'), 18, 'Trapping Rain Water', 'trapping-rain-water', 'HARD', 'https://leetcode.com/problems/trapping-rain-water/');

-- Strings (14)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 1,  'Valid Anagram', 'valid-anagram', 'EASY', 'https://leetcode.com/problems/valid-anagram/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 2,  'Valid Palindrome', 'valid-palindrome', 'EASY', 'https://leetcode.com/problems/valid-palindrome/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 3,  'Longest Common Prefix', 'longest-common-prefix', 'EASY', 'https://leetcode.com/problems/longest-common-prefix/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 4,  'Longest Substring Without Repeating Characters', 'longest-substring-without-repeating-characters', 'MEDIUM', 'https://leetcode.com/problems/longest-substring-without-repeating-characters/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 5,  'Group Anagrams', 'group-anagrams', 'MEDIUM', 'https://leetcode.com/problems/group-anagrams/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 6,  'Longest Palindromic Substring', 'longest-palindromic-substring', 'MEDIUM', 'https://leetcode.com/problems/longest-palindromic-substring/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 7,  'Palindromic Substrings', 'palindromic-substrings', 'MEDIUM', 'https://leetcode.com/problems/palindromic-substrings/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 8,  'Longest Repeating Character Replacement', 'longest-repeating-character-replacement', 'MEDIUM', 'https://leetcode.com/problems/longest-repeating-character-replacement/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 9,  'Encode and Decode Strings', 'encode-and-decode-strings', 'MEDIUM', 'https://leetcode.com/problems/encode-and-decode-strings/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 10, 'String to Integer (atoi)', 'string-to-integer-atoi', 'MEDIUM', 'https://leetcode.com/problems/string-to-integer-atoi/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 11, 'Find All Anagrams in a String', 'find-all-anagrams-in-a-string', 'MEDIUM', 'https://leetcode.com/problems/find-all-anagrams-in-a-string/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 12, 'Repeated DNA Sequences', 'repeated-dna-sequences', 'MEDIUM', 'https://leetcode.com/problems/repeated-dna-sequences/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 13, 'Zigzag Conversion', 'zigzag-conversion', 'MEDIUM', 'https://leetcode.com/problems/zigzag-conversion/'),
  ((SELECT id FROM dsa_topics WHERE slug='strings'), 14, 'Minimum Window Substring', 'minimum-window-substring', 'HARD', 'https://leetcode.com/problems/minimum-window-substring/');

-- Two Pointers (8)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 1, 'Reverse String', 'reverse-string', 'EASY', 'https://leetcode.com/problems/reverse-string/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 2, 'Merge Sorted Array', 'merge-sorted-array', 'EASY', 'https://leetcode.com/problems/merge-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 3, 'Move Zeroes', 'move-zeroes', 'EASY', 'https://leetcode.com/problems/move-zeroes/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 4, 'Remove Duplicates from Sorted Array', 'remove-duplicates-from-sorted-array', 'EASY', 'https://leetcode.com/problems/remove-duplicates-from-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 5, 'Squares of a Sorted Array', 'squares-of-a-sorted-array', 'EASY', 'https://leetcode.com/problems/squares-of-a-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 6, 'Intersection of Two Arrays', 'intersection-of-two-arrays', 'EASY', 'https://leetcode.com/problems/intersection-of-two-arrays/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 7, 'Two Sum II - Input Array Is Sorted', 'two-sum-ii-input-array-is-sorted', 'MEDIUM', 'https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/'),
  ((SELECT id FROM dsa_topics WHERE slug='two-pointers'), 8, '4Sum', '4sum', 'MEDIUM', 'https://leetcode.com/problems/4sum/');

-- Sliding Window (8)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 1, 'Minimum Size Subarray Sum', 'minimum-size-subarray-sum', 'MEDIUM', 'https://leetcode.com/problems/minimum-size-subarray-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 2, 'Fruit Into Baskets', 'fruit-into-baskets', 'MEDIUM', 'https://leetcode.com/problems/fruit-into-baskets/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 3, 'Longest Substring with At Most K Distinct Characters', 'longest-substring-with-at-most-k-distinct-characters', 'MEDIUM', 'https://leetcode.com/problems/longest-substring-with-at-most-k-distinct-characters/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 4, 'Permutation in String', 'permutation-in-string', 'MEDIUM', 'https://leetcode.com/problems/permutation-in-string/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 5, 'Max Consecutive Ones III', 'max-consecutive-ones-iii', 'MEDIUM', 'https://leetcode.com/problems/max-consecutive-ones-iii/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 6, 'Subarray Product Less Than K', 'subarray-product-less-than-k', 'MEDIUM', 'https://leetcode.com/problems/subarray-product-less-than-k/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 7, 'Sliding Window Maximum', 'sliding-window-maximum', 'HARD', 'https://leetcode.com/problems/sliding-window-maximum/'),
  ((SELECT id FROM dsa_topics WHERE slug='sliding-window'), 8, 'Substring with Concatenation of All Words', 'substring-with-concatenation-of-all-words', 'HARD', 'https://leetcode.com/problems/substring-with-concatenation-of-all-words/');

-- Binary Search (8)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 1, 'Binary Search', 'binary-search', 'EASY', 'https://leetcode.com/problems/binary-search/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 2, 'Search Insert Position', 'search-insert-position', 'EASY', 'https://leetcode.com/problems/search-insert-position/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 3, 'Search in Rotated Sorted Array', 'search-in-rotated-sorted-array', 'MEDIUM', 'https://leetcode.com/problems/search-in-rotated-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 4, 'Find Minimum in Rotated Sorted Array', 'find-minimum-in-rotated-sorted-array', 'MEDIUM', 'https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 5, 'Find First and Last Position of Element in Sorted Array', 'find-first-and-last-position-of-element-in-sorted-array', 'MEDIUM', 'https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 6, 'Koko Eating Bananas', 'koko-eating-bananas', 'MEDIUM', 'https://leetcode.com/problems/koko-eating-bananas/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 7, 'Time Based Key-Value Store', 'time-based-key-value-store', 'MEDIUM', 'https://leetcode.com/problems/time-based-key-value-store/'),
  ((SELECT id FROM dsa_topics WHERE slug='binary-search'), 8, 'Median of Two Sorted Arrays', 'median-of-two-sorted-arrays', 'HARD', 'https://leetcode.com/problems/median-of-two-sorted-arrays/');

-- Linked List (10)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 1,  'Reverse Linked List', 'reverse-linked-list', 'EASY', 'https://leetcode.com/problems/reverse-linked-list/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 2,  'Merge Two Sorted Lists', 'merge-two-sorted-lists', 'EASY', 'https://leetcode.com/problems/merge-two-sorted-lists/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 3,  'Linked List Cycle', 'linked-list-cycle', 'EASY', 'https://leetcode.com/problems/linked-list-cycle/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 4,  'Remove Nth Node From End of List', 'remove-nth-node-from-end-of-list', 'MEDIUM', 'https://leetcode.com/problems/remove-nth-node-from-end-of-list/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 5,  'Reorder List', 'reorder-list', 'MEDIUM', 'https://leetcode.com/problems/reorder-list/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 6,  'Add Two Numbers', 'add-two-numbers', 'MEDIUM', 'https://leetcode.com/problems/add-two-numbers/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 7,  'Copy List with Random Pointer', 'copy-list-with-random-pointer', 'MEDIUM', 'https://leetcode.com/problems/copy-list-with-random-pointer/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 8,  'LRU Cache', 'lru-cache', 'MEDIUM', 'https://leetcode.com/problems/lru-cache/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 9,  'Merge k Sorted Lists', 'merge-k-sorted-lists', 'HARD', 'https://leetcode.com/problems/merge-k-sorted-lists/'),
  ((SELECT id FROM dsa_topics WHERE slug='linked-list'), 10, 'Reverse Nodes in k-Group', 'reverse-nodes-in-k-group', 'HARD', 'https://leetcode.com/problems/reverse-nodes-in-k-group/');

-- Stacks & Queues (8)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 1, 'Valid Parentheses', 'valid-parentheses', 'EASY', 'https://leetcode.com/problems/valid-parentheses/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 2, 'Implement Queue using Stacks', 'implement-queue-using-stacks', 'EASY', 'https://leetcode.com/problems/implement-queue-using-stacks/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 3, 'Min Stack', 'min-stack', 'MEDIUM', 'https://leetcode.com/problems/min-stack/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 4, 'Evaluate Reverse Polish Notation', 'evaluate-reverse-polish-notation', 'MEDIUM', 'https://leetcode.com/problems/evaluate-reverse-polish-notation/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 5, 'Daily Temperatures', 'daily-temperatures', 'MEDIUM', 'https://leetcode.com/problems/daily-temperatures/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 6, 'Car Fleet', 'car-fleet', 'MEDIUM', 'https://leetcode.com/problems/car-fleet/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 7, 'Design Circular Queue', 'design-circular-queue', 'MEDIUM', 'https://leetcode.com/problems/design-circular-queue/'),
  ((SELECT id FROM dsa_topics WHERE slug='stacks-queues'), 8, 'Largest Rectangle in Histogram', 'largest-rectangle-in-histogram', 'HARD', 'https://leetcode.com/problems/largest-rectangle-in-histogram/');

-- Trees (14)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 1,  'Invert Binary Tree', 'invert-binary-tree', 'EASY', 'https://leetcode.com/problems/invert-binary-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 2,  'Maximum Depth of Binary Tree', 'maximum-depth-of-binary-tree', 'EASY', 'https://leetcode.com/problems/maximum-depth-of-binary-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 3,  'Same Tree', 'same-tree', 'EASY', 'https://leetcode.com/problems/same-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 4,  'Subtree of Another Tree', 'subtree-of-another-tree', 'EASY', 'https://leetcode.com/problems/subtree-of-another-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 5,  'Diameter of Binary Tree', 'diameter-of-binary-tree', 'EASY', 'https://leetcode.com/problems/diameter-of-binary-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 6,  'Balanced Binary Tree', 'balanced-binary-tree', 'EASY', 'https://leetcode.com/problems/balanced-binary-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 7,  'Binary Tree Level Order Traversal', 'binary-tree-level-order-traversal', 'MEDIUM', 'https://leetcode.com/problems/binary-tree-level-order-traversal/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 8,  'Binary Tree Right Side View', 'binary-tree-right-side-view', 'MEDIUM', 'https://leetcode.com/problems/binary-tree-right-side-view/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 9,  'Count Good Nodes in Binary Tree', 'count-good-nodes-in-binary-tree', 'MEDIUM', 'https://leetcode.com/problems/count-good-nodes-in-binary-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 10, 'Validate Binary Search Tree', 'validate-binary-search-tree', 'MEDIUM', 'https://leetcode.com/problems/validate-binary-search-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 11, 'Kth Smallest Element in a BST', 'kth-smallest-element-in-a-bst', 'MEDIUM', 'https://leetcode.com/problems/kth-smallest-element-in-a-bst/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 12, 'Construct Binary Tree from Preorder and Inorder Traversal', 'construct-binary-tree-from-preorder-and-inorder-traversal', 'MEDIUM', 'https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 13, 'Binary Tree Maximum Path Sum', 'binary-tree-maximum-path-sum', 'HARD', 'https://leetcode.com/problems/binary-tree-maximum-path-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='trees'), 14, 'Serialize and Deserialize Binary Tree', 'serialize-and-deserialize-binary-tree', 'HARD', 'https://leetcode.com/problems/serialize-and-deserialize-binary-tree/');

-- Heaps (6)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 1, 'Last Stone Weight', 'last-stone-weight', 'EASY', 'https://leetcode.com/problems/last-stone-weight/'),
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 2, 'Kth Largest Element in an Array', 'kth-largest-element-in-an-array', 'MEDIUM', 'https://leetcode.com/problems/kth-largest-element-in-an-array/'),
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 3, 'K Closest Points to Origin', 'k-closest-points-to-origin', 'MEDIUM', 'https://leetcode.com/problems/k-closest-points-to-origin/'),
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 4, 'Top K Frequent Elements', 'top-k-frequent-elements', 'MEDIUM', 'https://leetcode.com/problems/top-k-frequent-elements/'),
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 5, 'Task Scheduler', 'task-scheduler', 'MEDIUM', 'https://leetcode.com/problems/task-scheduler/'),
  ((SELECT id FROM dsa_topics WHERE slug='heaps'), 6, 'Find Median from Data Stream', 'find-median-from-data-stream', 'HARD', 'https://leetcode.com/problems/find-median-from-data-stream/');

-- Backtracking (8)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 1, 'Subsets', 'subsets', 'MEDIUM', 'https://leetcode.com/problems/subsets/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 2, 'Combination Sum', 'combination-sum', 'MEDIUM', 'https://leetcode.com/problems/combination-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 3, 'Permutations', 'permutations', 'MEDIUM', 'https://leetcode.com/problems/permutations/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 4, 'Subsets II', 'subsets-ii', 'MEDIUM', 'https://leetcode.com/problems/subsets-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 5, 'Combination Sum II', 'combination-sum-ii', 'MEDIUM', 'https://leetcode.com/problems/combination-sum-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 6, 'Word Search', 'word-search', 'MEDIUM', 'https://leetcode.com/problems/word-search/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 7, 'Palindrome Partitioning', 'palindrome-partitioning', 'MEDIUM', 'https://leetcode.com/problems/palindrome-partitioning/'),
  ((SELECT id FROM dsa_topics WHERE slug='backtracking'), 8, 'N-Queens', 'n-queens', 'HARD', 'https://leetcode.com/problems/n-queens/');

-- Tries (4)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='tries'), 1, 'Implement Trie (Prefix Tree)', 'implement-trie-prefix-tree', 'MEDIUM', 'https://leetcode.com/problems/implement-trie-prefix-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='tries'), 2, 'Design Add and Search Words Data Structure', 'design-add-and-search-words-data-structure', 'MEDIUM', 'https://leetcode.com/problems/design-add-and-search-words-data-structure/'),
  ((SELECT id FROM dsa_topics WHERE slug='tries'), 3, 'Longest Word in Dictionary', 'longest-word-in-dictionary', 'EASY', 'https://leetcode.com/problems/longest-word-in-dictionary/'),
  ((SELECT id FROM dsa_topics WHERE slug='tries'), 4, 'Word Search II', 'word-search-ii', 'HARD', 'https://leetcode.com/problems/word-search-ii/');

-- Graphs (12)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 1,  'Number of Islands', 'number-of-islands', 'MEDIUM', 'https://leetcode.com/problems/number-of-islands/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 2,  'Clone Graph', 'clone-graph', 'MEDIUM', 'https://leetcode.com/problems/clone-graph/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 3,  'Max Area of Island', 'max-area-of-island', 'MEDIUM', 'https://leetcode.com/problems/max-area-of-island/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 4,  'Pacific Atlantic Water Flow', 'pacific-atlantic-water-flow', 'MEDIUM', 'https://leetcode.com/problems/pacific-atlantic-water-flow/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 5,  'Surrounded Regions', 'surrounded-regions', 'MEDIUM', 'https://leetcode.com/problems/surrounded-regions/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 6,  'Rotting Oranges', 'rotting-oranges', 'MEDIUM', 'https://leetcode.com/problems/rotting-oranges/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 7,  'Course Schedule', 'course-schedule', 'MEDIUM', 'https://leetcode.com/problems/course-schedule/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 8,  'Course Schedule II', 'course-schedule-ii', 'MEDIUM', 'https://leetcode.com/problems/course-schedule-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 9,  'Number of Connected Components in an Undirected Graph', 'number-of-connected-components-in-an-undirected-graph', 'MEDIUM', 'https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 10, 'Graph Valid Tree', 'graph-valid-tree', 'MEDIUM', 'https://leetcode.com/problems/graph-valid-tree/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 11, 'Word Ladder', 'word-ladder', 'HARD', 'https://leetcode.com/problems/word-ladder/'),
  ((SELECT id FROM dsa_topics WHERE slug='graphs'), 12, 'Alien Dictionary', 'alien-dictionary', 'HARD', 'https://leetcode.com/problems/alien-dictionary/');

-- Greedy (6)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 1, 'Jump Game', 'jump-game', 'MEDIUM', 'https://leetcode.com/problems/jump-game/'),
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 2, 'Jump Game II', 'jump-game-ii', 'MEDIUM', 'https://leetcode.com/problems/jump-game-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 3, 'Gas Station', 'gas-station', 'MEDIUM', 'https://leetcode.com/problems/gas-station/'),
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 4, 'Hand of Straights', 'hand-of-straights', 'MEDIUM', 'https://leetcode.com/problems/hand-of-straights/'),
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 5, 'Partition Labels', 'partition-labels', 'MEDIUM', 'https://leetcode.com/problems/partition-labels/'),
  ((SELECT id FROM dsa_topics WHERE slug='greedy'), 6, 'Valid Parenthesis String', 'valid-parenthesis-string', 'MEDIUM', 'https://leetcode.com/problems/valid-parenthesis-string/');

-- Intervals (5)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='intervals'), 1, 'Meeting Rooms', 'meeting-rooms', 'EASY', 'https://leetcode.com/problems/meeting-rooms/'),
  ((SELECT id FROM dsa_topics WHERE slug='intervals'), 2, 'Merge Intervals', 'merge-intervals', 'MEDIUM', 'https://leetcode.com/problems/merge-intervals/'),
  ((SELECT id FROM dsa_topics WHERE slug='intervals'), 3, 'Insert Interval', 'insert-interval', 'MEDIUM', 'https://leetcode.com/problems/insert-interval/'),
  ((SELECT id FROM dsa_topics WHERE slug='intervals'), 4, 'Non-overlapping Intervals', 'non-overlapping-intervals', 'MEDIUM', 'https://leetcode.com/problems/non-overlapping-intervals/'),
  ((SELECT id FROM dsa_topics WHERE slug='intervals'), 5, 'Meeting Rooms II', 'meeting-rooms-ii', 'MEDIUM', 'https://leetcode.com/problems/meeting-rooms-ii/');

-- 1-D DP (10)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 1,  'Climbing Stairs', 'climbing-stairs', 'EASY', 'https://leetcode.com/problems/climbing-stairs/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 2,  'Min Cost Climbing Stairs', 'min-cost-climbing-stairs', 'EASY', 'https://leetcode.com/problems/min-cost-climbing-stairs/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 3,  'House Robber', 'house-robber', 'MEDIUM', 'https://leetcode.com/problems/house-robber/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 4,  'House Robber II', 'house-robber-ii', 'MEDIUM', 'https://leetcode.com/problems/house-robber-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 5,  'Decode Ways', 'decode-ways', 'MEDIUM', 'https://leetcode.com/problems/decode-ways/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 6,  'Word Break', 'word-break', 'MEDIUM', 'https://leetcode.com/problems/word-break/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 7,  'Coin Change', 'coin-change', 'MEDIUM', 'https://leetcode.com/problems/coin-change/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 8,  'Longest Increasing Subsequence', 'longest-increasing-subsequence', 'MEDIUM', 'https://leetcode.com/problems/longest-increasing-subsequence/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 9,  'Partition Equal Subset Sum', 'partition-equal-subset-sum', 'MEDIUM', 'https://leetcode.com/problems/partition-equal-subset-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-1d'), 10, 'Integer Break', 'integer-break', 'MEDIUM', 'https://leetcode.com/problems/integer-break/');

-- 2-D DP (6)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 1, 'Unique Paths', 'unique-paths', 'MEDIUM', 'https://leetcode.com/problems/unique-paths/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 2, 'Minimum Path Sum', 'minimum-path-sum', 'MEDIUM', 'https://leetcode.com/problems/minimum-path-sum/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 3, 'Longest Common Subsequence', 'longest-common-subsequence', 'MEDIUM', 'https://leetcode.com/problems/longest-common-subsequence/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 4, 'Coin Change II', 'coin-change-ii', 'MEDIUM', 'https://leetcode.com/problems/coin-change-ii/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 5, 'Edit Distance', 'edit-distance', 'MEDIUM', 'https://leetcode.com/problems/edit-distance/'),
  ((SELECT id FROM dsa_topics WHERE slug='dp-2d'), 6, 'Burst Balloons', 'burst-balloons', 'HARD', 'https://leetcode.com/problems/burst-balloons/');

-- Bit Manipulation (7)
INSERT INTO dsa_questions (topic_id, position, title, slug, difficulty, leetcode_url) VALUES
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 1, 'Single Number', 'single-number', 'EASY', 'https://leetcode.com/problems/single-number/'),
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 2, 'Number of 1 Bits', 'number-of-1-bits', 'EASY', 'https://leetcode.com/problems/number-of-1-bits/'),
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 3, 'Counting Bits', 'counting-bits', 'EASY', 'https://leetcode.com/problems/counting-bits/'),
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 4, 'Reverse Bits', 'reverse-bits', 'EASY', 'https://leetcode.com/problems/reverse-bits/'),
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 5, 'Sum of Two Integers', 'sum-of-two-integers', 'MEDIUM', 'https://leetcode.com/problems/sum-of-two-integers/'),
  ((SELECT id FROM dsa_topics WHERE slug='bit-manipulation'), 6, 'Reverse Integer', 'reverse-integer', 'MEDIUM', 'https://leetcode.com/problems/reverse-integer/');

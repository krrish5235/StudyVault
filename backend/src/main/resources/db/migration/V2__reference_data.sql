-- =====================================================================
-- StudyVault - reference data (roles, default categories, site settings)
-- No business data (products) is seeded: products are created via admin.
-- =====================================================================

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

INSERT INTO categories (name, slug, sort_order) VALUES
  ('Java Programming',          'java-programming',          1),
  ('Spring Boot',               'spring-boot',               2),
  ('Databases & SQL',           'databases-sql',             3),
  ('Data Structures & Algorithms', 'dsa',                    4),
  ('Python',                    'python',                    5),
  ('C / C++',                   'c-cpp',                     6),
  ('System Design',             'system-design',             7),
  ('Cloud & DevOps',            'cloud-devops',              8),
  ('Interview Preparation',     'interview-preparation',     9),
  ('Placement Preparation',     'placement-preparation',     10),
  ('Web Development',           'web-development',           11),
  ('Cheat Sheets',              'cheat-sheets',              12),
  ('E-books',                   'e-books',                   13),
  ('Project Resources',         'project-resources',         14);

INSERT INTO site_settings (key, value) VALUES
  ('site.name',        '"StudyVault"'),
  ('site.tagline',     '"Premium notes for serious engineers"'),
  ('hero.title',       '"Master the concepts. Own the interview."'),
  ('hero.subtitle',    '"Hand-crafted programming notes, e-books and cheat sheets - written by engineers, for engineers."'),
  ('hero.ctaText',     '"Browse the catalog"'),
  ('about.content',    '"StudyVault is a curated marketplace of programming study material. Every product is written, reviewed and versioned by the store owner."'),
  ('contact.email',    '"support@studyvault.local"'),
  ('social.links',     '{"linkedin":"","github":"","twitter":"","youtube":""}'),
  ('footer.text',      '"Built for engineers who take their craft seriously."'),
  ('announcement',     '{"enabled":false,"text":"","link":""}');

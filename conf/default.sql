
CREATE TABLE IF NOT EXISTS projects(
  id VARCHAR(36) PRIMARY KEY,
  name TEXT,
  problem TEXT,
  default_algorithm VARCHAR(36),
);

CREATE TABLE IF NOT EXISTS algorithms(
  id VARCHAR(36) PRIMARY KEY,
  backend TEXT,
  project_id VARCHAR(36) REFERENCES projects(id)
);


CREATE TABLE IF NOT EXISTS predictions(
  id VARCHAR(36),
  features TEXT NOT NULL,
  predicted_labels TEXT NOT NULL,
  true_labels TEXT 
);


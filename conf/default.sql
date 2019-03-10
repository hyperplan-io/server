
CREATE TABLE IF NOT EXISTS projects(
  id VARCHAR(36) PRIMARY KEY,
  name TEXT NOT NULL,
  problem TEXT NOT NULL,
  algorithm_policy VARCHAR(36) NOT NULL,
  feature_class TEXT NOT NULL,
  label_class TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS algorithms(
  id VARCHAR(36) PRIMARY KEY,
  backend TEXT NOT NULL ,
  project_id VARCHAR(36) REFERENCES projects(id) NOT NULL 
);


CREATE TABLE IF NOT EXISTS predictions(
  id VARCHAR(36),
  features TEXT NOT NULL,
  predicted_labels TEXT NOT NULL,
  true_labels TEXT 
);


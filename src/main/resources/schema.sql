
CREATE TABLE IF NOT EXISTS project_config (
	id INT AUTO_INCREMENT(1, 1) PRIMARY KEY, 
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	project_id INT NOT NULL,
	hook_id INT NOT NULL, 
	enabled BOOLEAN DEFAULT TRUE,
	mail_to_type VARCHAR(16) DEFAULT 'NONE',
	additional_mail_to VARCHAR(1024),
	exclude_mail_to VARCHAR(1024),
	include_default_mail_to BOOLEAN DEFAULT FALSE);

CREATE UNIQUE INDEX IF NOT EXISTS project_config_index ON project_config(project_id);


CREATE TABLE IF NOT EXISTS push (
	id INT AUTO_INCREMENT(1, 1) PRIMARY KEY,
	received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	user_id INT NOT NULL,
	branch VARCHAR(64) NOT NULL,
	project_id INT NOT NULL,
	before VARCHAR(64),
	after VARCHAR(64),
	merge_request_id INT DEFAULT 0,
	merge_status_date TIMESTAMP,
	merge_state VARCHAR(32),
	merge_status VARCHAR(32),
	merged_by_id INT);

CREATE INDEX IF NOT EXISTS push_index ON push(user_id, project_id, branch, merge_request_id);


CREATE TABLE IF NOT EXISTS merge_spec (
	id INT AUTO_INCREMENT(1, 1) PRIMARY KEY,
	project_config_id INT,
	project_id INT,
	branch_regex VARCHAR(128) NOT NULL,
	target_branch VARCHAR(64) NOT NULL,
	FOREIGN KEY (project_config_id) REFERENCES project_config(id) ON DELETE CASCADE);

CREATE UNIQUE INDEX IF NOT EXISTS merge_spec_index ON merge_spec(project_id, branch_regex, target_branch);
CREATE TABLE fee_rule_write_lock (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);

INSERT INTO fee_rule_write_lock (id) VALUES (1);

CREATE TABLE interchange_fees (
    brand VARCHAR(16) NOT NULL,
    card_type VARCHAR(16) NOT NULL DEFAULT '*',
    region VARCHAR(4) NOT NULL DEFAULT '*',
    mcc VARCHAR(4) NOT NULL DEFAULT '*',
    flat_fee DECIMAL(19,4) NOT NULL DEFAULT 0,
    percentage_fee DECIMAL(7,4) NOT NULL DEFAULT 0,
    PRIMARY KEY (brand, card_type, region, mcc)
);

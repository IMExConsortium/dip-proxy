CREATE TABLE dxf_record (
    pk serial primary key,
    provider character varying(20) DEFAULT ''::character varying NOT NULL,
    service character varying(20) NOT NULL,
    ns character varying(20) NOT NULL,
    ac character varying(20) NOT NULL,
    detail character varying(5),
    dxf text NOT NULL,
    create_time timestamp without time zone NOT NULL,
    expire_time timestamp without time zone NOT NULL
);

CREATE TABLE native_record (
    pk serial primary key,
    provider character varying(20) DEFAULT ''::character varying NOT NULL,
    service character varying(20) DEFAULT ''::character varying NOT NULL,
    ns character varying(20) DEFAULT ''::character varying NOT NULL,
    ac character varying(20) DEFAULT ''::character varying NOT NULL,
    native_xml text NOT NULL,
    create_time timestamp without time zone NOT NULL,
    expire_time timestamp without time zone NOT NULL,
    ttl integer DEFAULT 0 NOT NULL
);

CREATE TABLE native_audit (
    pk serial primary key,
    provider character varying(20) DEFAULT ''::character varying NOT NULL,
    service character varying(20) DEFAULT ''::character varying NOT NULL,
    ns character varying(20) DEFAULT ''::character varying NOT NULL,
    ac character varying(20) DEFAULT ''::character varying NOT NULL,
    time timestamp without time zone NOT NULL,
    delay integer DEFAULT 0 NOT NULL
);

CREATE INDEX native_idx1 ON native_record (provider, service, ns, ac);
CREATE INDEX native_idx2 ON native_record (expire_time);

CREATE INDEX dxf_idx1 ON dxf_record (provider, service, ns, ac, detail);
CREATE INDEX dxf_idx2 ON dxf_record (expire_time);

CREATE INDEX I11 ON native_audit (service);
CREATE INDEX I12 ON native_audit (provider, service);
CREATE INDEX I13 ON native_audit (ns);
CREATE INDEX I14 ON native_audit (ac, ns);
CREATE INDEX I15 ON native_audit (time);
CREATE INDEX I16 ON native_audit (time, provider);
CREATE INDEX I17 ON native_audit (delay);

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6


--
-- Name: logistica; Type: SCHEMA; Schema: -; Owner: -
--





--
-- Name: camion; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE camion (
    patente character varying(255) NOT NULL,
    alturam double precision NOT NULL,
    capacidadcargakg double precision NOT NULL,
    volumenm3 double precision NOT NULL
);


--
-- Name: destino_donaciones; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE destino_donaciones (
    destino_id bigint NOT NULL,
    donacion_id uuid NOT NULL
);


--
-- Name: destinos; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE destinos (
    id bigint NOT NULL,
    direccion character varying(255) NOT NULL,
    entidad_id bigint NOT NULL,
    ruta_id uuid,
    orden integer
);


--
-- Name: destinos_id_seq; Type: SEQUENCE; Schema: logistica; Owner: -
--

CREATE SEQUENCE destinos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: destinos_id_seq; Type: SEQUENCE OWNED BY; Schema: logistica; Owner: -
--

ALTER SEQUENCE destinos_id_seq OWNED BY destinos.id;


--
-- Name: logisticsdemoversion; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE logisticsdemoversion (
    id character varying(255) NOT NULL
);


--
-- Name: repartidores; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE repartidores (
    id uuid NOT NULL,
    email character varying(255) NOT NULL,
    hashcontrasenia character varying(255) NOT NULL,
    nombre character varying(255) NOT NULL
);


--
-- Name: ruta_repartidores; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE ruta_repartidores (
    ruta_id uuid NOT NULL,
    repartidor_id uuid
);


--
-- Name: rutas; Type: TABLE; Schema: logistica; Owner: -
--

CREATE TABLE rutas (
    id uuid NOT NULL,
    encurso boolean NOT NULL,
    camion_id character varying(255)
);


--
-- Name: destinos id; Type: DEFAULT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY destinos ALTER COLUMN id SET DEFAULT nextval('destinos_id_seq'::regclass);


--
-- Name: camion camion_pkey; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY camion
    ADD CONSTRAINT camion_pkey PRIMARY KEY (patente);


--
-- Name: destinos destinos_pkey; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY destinos
    ADD CONSTRAINT destinos_pkey PRIMARY KEY (id);


--
-- Name: logisticsdemoversion logisticsdemoversion_pkey; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY logisticsdemoversion
    ADD CONSTRAINT logisticsdemoversion_pkey PRIMARY KEY (id);


--
-- Name: repartidores repartidores_pkey; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY repartidores
    ADD CONSTRAINT repartidores_pkey PRIMARY KEY (id);


--
-- Name: rutas rutas_pkey; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY rutas
    ADD CONSTRAINT rutas_pkey PRIMARY KEY (id);


--
-- Name: repartidores uk_obieu0dt04oygoe2mam6i6myy; Type: CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY repartidores
    ADD CONSTRAINT uk_obieu0dt04oygoe2mam6i6myy UNIQUE (email);


--
-- Name: ruta_repartidores fke5ubpp9e70qwc5t6rlwhyk7f2; Type: FK CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY ruta_repartidores
    ADD CONSTRAINT fke5ubpp9e70qwc5t6rlwhyk7f2 FOREIGN KEY (ruta_id) REFERENCES rutas(id);


--
-- Name: destinos fke6dvg2f987qj4wfpdwl6tuxge; Type: FK CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY destinos
    ADD CONSTRAINT fke6dvg2f987qj4wfpdwl6tuxge FOREIGN KEY (ruta_id) REFERENCES rutas(id);


--
-- Name: rutas fkqsl6gadmm7n761lb5t8f3jrur; Type: FK CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY rutas
    ADD CONSTRAINT fkqsl6gadmm7n761lb5t8f3jrur FOREIGN KEY (camion_id) REFERENCES camion(patente);


--
-- Name: destino_donaciones fktk374369dpi5fldyfb60gtobr; Type: FK CONSTRAINT; Schema: logistica; Owner: -
--

ALTER TABLE ONLY destino_donaciones
    ADD CONSTRAINT fktk374369dpi5fldyfb60gtobr FOREIGN KEY (destino_id) REFERENCES destinos(id);


--
-- PostgreSQL database dump complete
--



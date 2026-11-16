--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6


--
-- Name: donaciones; Type: SCHEMA; Schema: -; Owner: -
--





--
-- Name: aviso_bandeja; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE aviso_bandeja (
    id uuid NOT NULL,
    donanteid uuid,
    entidadid bigint,
    fecha timestamp without time zone NOT NULL,
    leido boolean NOT NULL,
    mensaje character varying(1000) NOT NULL
);


--
-- Name: bien; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE bien (
    id bigint NOT NULL,
    cantidad double precision NOT NULL,
    descripcion character varying(255),
    estadodeuso character varying(255),
    fechavencimiento date,
    foto character varying(255),
    unidad character varying(255),
    subcategoria_id bigint NOT NULL,
    donacion_id uuid
);


--
-- Name: bien_id_seq; Type: SEQUENCE; Schema: donaciones; Owner: -
--

CREATE SEQUENCE bien_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bien_id_seq; Type: SEQUENCE OWNED BY; Schema: donaciones; Owner: -
--

ALTER SEQUENCE bien_id_seq OWNED BY bien.id;


--
-- Name: demodatasetversion; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE demodatasetversion (
    id character varying(255) NOT NULL
);


--
-- Name: donacion; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE donacion (
    id uuid NOT NULL,
    estado character varying(255) NOT NULL,
    idcamion character varying(255),
    donante_id uuid,
    entidad_id bigint
);


--
-- Name: donacion_fotos; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE donacion_fotos (
    donacion_id uuid NOT NULL,
    foto character varying(255)
);


--
-- Name: donante; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE donante (
    id uuid NOT NULL,
    documento character varying(255),
    numdocumento character varying(255),
    contacto_predeterminado_id bigint
);


--
-- Name: entidadbeneficiaria; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE entidadbeneficiaria (
    id bigint NOT NULL,
    direccion character varying(255),
    latitud double precision,
    longitud double precision,
    razonsocial character varying(255),
    telefono character varying(255),
    ubicacionintentada boolean NOT NULL
);


--
-- Name: entidadbeneficiaria_id_seq; Type: SEQUENCE; Schema: donaciones; Owner: -
--

CREATE SEQUENCE entidadbeneficiaria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entidadbeneficiaria_id_seq; Type: SEQUENCE OWNED BY; Schema: donaciones; Owner: -
--

ALTER SEQUENCE entidadbeneficiaria_id_seq OWNED BY entidadbeneficiaria.id;


--
-- Name: historialestado; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE historialestado (
    donacion_id uuid NOT NULL,
    estadoalcanzado character varying(255) NOT NULL,
    fecha timestamp without time zone,
    justificacion character varying(255),
    orden integer NOT NULL
);


--
-- Name: medio_de_contacto_seq; Type: SEQUENCE; Schema: donaciones; Owner: -
--

CREATE SEQUENCE medio_de_contacto_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mediodecontacto; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE mediodecontacto (
    dtype character varying(31) NOT NULL,
    id bigint NOT NULL,
    contacto character varying(255) NOT NULL,
    entidad_id bigint,
    donante_id uuid
);


--
-- Name: necesidad; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE necesidad (
    dtype character varying(31) NOT NULL,
    id uuid NOT NULL,
    cantidadobjetivo double precision NOT NULL,
    cantidadrecibida double precision NOT NULL,
    descripcion character varying(255),
    unidad character varying(255),
    periodo character varying(255),
    subcategoria_id bigint NOT NULL,
    entidad_recurrente_id bigint,
    entidad_id bigint
);


--
-- Name: personahumana; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE personahumana (
    apellido character varying(255),
    direccion character varying(255),
    edad integer,
    genero character varying(255),
    nombre character varying(255),
    id uuid NOT NULL
);


--
-- Name: personajejuridica; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE personajejuridica (
    razonsocial character varying(255),
    rubro character varying(255),
    tipoorganizacion character varying(255),
    id uuid NOT NULL
);


--
-- Name: propuesta_candidatos; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE propuesta_candidatos (
    propuesta_id uuid NOT NULL,
    entidad_id bigint NOT NULL,
    puntaje integer NOT NULL,
    orden integer NOT NULL
);


--
-- Name: propuestadeasignacion; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE propuestadeasignacion (
    id uuid NOT NULL,
    pendiente boolean NOT NULL,
    donacion_id uuid NOT NULL
);


--
-- Name: representante; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE representante (
    id bigint NOT NULL,
    apellido character varying(255),
    nombre character varying(255),
    donante_id uuid
);


--
-- Name: representante_id_seq; Type: SEQUENCE; Schema: donaciones; Owner: -
--

CREATE SEQUENCE representante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: representante_id_seq; Type: SEQUENCE OWNED BY; Schema: donaciones; Owner: -
--

ALTER SEQUENCE representante_id_seq OWNED BY representante.id;


--
-- Name: subcategoria; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE subcategoria (
    id bigint NOT NULL,
    categoria character varying(255),
    nombre character varying(255)
);


--
-- Name: subcategoria_id_seq; Type: SEQUENCE; Schema: donaciones; Owner: -
--

CREATE SEQUENCE subcategoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: subcategoria_id_seq; Type: SEQUENCE OWNED BY; Schema: donaciones; Owner: -
--

ALTER SEQUENCE subcategoria_id_seq OWNED BY subcategoria.id;


--
-- Name: usuario_roles; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE usuario_roles (
    usuario_id uuid NOT NULL,
    rol character varying(255)
);


--
-- Name: usuarios; Type: TABLE; Schema: donaciones; Owner: -
--

CREATE TABLE usuarios (
    id uuid NOT NULL,
    donanteid uuid,
    email character varying(255) NOT NULL,
    entidadid bigint,
    passwordhash character varying(255) NOT NULL
);


--
-- Name: bien id; Type: DEFAULT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY bien ALTER COLUMN id SET DEFAULT nextval('bien_id_seq'::regclass);


--
-- Name: entidadbeneficiaria id; Type: DEFAULT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY entidadbeneficiaria ALTER COLUMN id SET DEFAULT nextval('entidadbeneficiaria_id_seq'::regclass);


--
-- Name: representante id; Type: DEFAULT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY representante ALTER COLUMN id SET DEFAULT nextval('representante_id_seq'::regclass);


--
-- Name: subcategoria id; Type: DEFAULT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY subcategoria ALTER COLUMN id SET DEFAULT nextval('subcategoria_id_seq'::regclass);


--
-- Name: aviso_bandeja aviso_bandeja_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY aviso_bandeja
    ADD CONSTRAINT aviso_bandeja_pkey PRIMARY KEY (id);


--
-- Name: bien bien_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY bien
    ADD CONSTRAINT bien_pkey PRIMARY KEY (id);


--
-- Name: demodatasetversion demodatasetversion_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY demodatasetversion
    ADD CONSTRAINT demodatasetversion_pkey PRIMARY KEY (id);


--
-- Name: donacion donacion_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donacion
    ADD CONSTRAINT donacion_pkey PRIMARY KEY (id);


--
-- Name: donante donante_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donante
    ADD CONSTRAINT donante_pkey PRIMARY KEY (id);


--
-- Name: entidadbeneficiaria entidadbeneficiaria_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY entidadbeneficiaria
    ADD CONSTRAINT entidadbeneficiaria_pkey PRIMARY KEY (id);


--
-- Name: historialestado historialestado_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY historialestado
    ADD CONSTRAINT historialestado_pkey PRIMARY KEY (donacion_id, orden);


--
-- Name: mediodecontacto mediodecontacto_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY mediodecontacto
    ADD CONSTRAINT mediodecontacto_pkey PRIMARY KEY (id);


--
-- Name: necesidad necesidad_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY necesidad
    ADD CONSTRAINT necesidad_pkey PRIMARY KEY (id);


--
-- Name: personahumana personahumana_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY personahumana
    ADD CONSTRAINT personahumana_pkey PRIMARY KEY (id);


--
-- Name: personajejuridica personajejuridica_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY personajejuridica
    ADD CONSTRAINT personajejuridica_pkey PRIMARY KEY (id);


--
-- Name: propuesta_candidatos propuesta_candidatos_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuesta_candidatos
    ADD CONSTRAINT propuesta_candidatos_pkey PRIMARY KEY (propuesta_id, orden);


--
-- Name: propuestadeasignacion propuestadeasignacion_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuestadeasignacion
    ADD CONSTRAINT propuestadeasignacion_pkey PRIMARY KEY (id);


--
-- Name: representante representante_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY representante
    ADD CONSTRAINT representante_pkey PRIMARY KEY (id);


--
-- Name: subcategoria subcategoria_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY subcategoria
    ADD CONSTRAINT subcategoria_pkey PRIMARY KEY (id);


--
-- Name: propuestadeasignacion uk_71i122uki1iit27rdiudvplot; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuestadeasignacion
    ADD CONSTRAINT uk_71i122uki1iit27rdiudvplot UNIQUE (donacion_id);


--
-- Name: usuarios uk_kfsp0s1tflm1cwlj8idhqsad0; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY usuarios
    ADD CONSTRAINT uk_kfsp0s1tflm1cwlj8idhqsad0 UNIQUE (email);


--
-- Name: subcategoria uk_subcategoria_nombre; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY subcategoria
    ADD CONSTRAINT uk_subcategoria_nombre UNIQUE (nombre);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: ix_aviso_donante; Type: INDEX; Schema: donaciones; Owner: -
--

CREATE INDEX ix_aviso_donante ON aviso_bandeja USING btree (donanteid);


--
-- Name: ix_aviso_entidad; Type: INDEX; Schema: donaciones; Owner: -
--

CREATE INDEX ix_aviso_entidad ON aviso_bandeja USING btree (entidadid);


--
-- Name: mediodecontacto fk2djt1smqm7nf072lo900mpkn8; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY mediodecontacto
    ADD CONSTRAINT fk2djt1smqm7nf072lo900mpkn8 FOREIGN KEY (entidad_id) REFERENCES entidadbeneficiaria(id);


--
-- Name: necesidad fk2tpkwi1hvdptm074jdgv1hw2g; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY necesidad
    ADD CONSTRAINT fk2tpkwi1hvdptm074jdgv1hw2g FOREIGN KEY (entidad_recurrente_id) REFERENCES entidadbeneficiaria(id);


--
-- Name: historialestado fk4s7pdavwbjre6gjdp4s7f22m8; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY historialestado
    ADD CONSTRAINT fk4s7pdavwbjre6gjdp4s7f22m8 FOREIGN KEY (donacion_id) REFERENCES donacion(id);


--
-- Name: mediodecontacto fk4sct9ijaewhpjmmo6uxbpkevg; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY mediodecontacto
    ADD CONSTRAINT fk4sct9ijaewhpjmmo6uxbpkevg FOREIGN KEY (donante_id) REFERENCES donante(id);


--
-- Name: donacion fk88y8u81eaw4ld1r0vl1vlj6x6; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donacion
    ADD CONSTRAINT fk88y8u81eaw4ld1r0vl1vlj6x6 FOREIGN KEY (entidad_id) REFERENCES entidadbeneficiaria(id);


--
-- Name: personahumana fk912wghutuep327gtu7t2eguuy; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY personahumana
    ADD CONSTRAINT fk912wghutuep327gtu7t2eguuy FOREIGN KEY (id) REFERENCES donante(id);


--
-- Name: necesidad fka01nuq47ipeuj4lioihv423jt; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY necesidad
    ADD CONSTRAINT fka01nuq47ipeuj4lioihv423jt FOREIGN KEY (subcategoria_id) REFERENCES subcategoria(id);


--
-- Name: propuestadeasignacion fka8p55p4kwppl23fu2r4snfo1h; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuestadeasignacion
    ADD CONSTRAINT fka8p55p4kwppl23fu2r4snfo1h FOREIGN KEY (donacion_id) REFERENCES donacion(id);


--
-- Name: bien fkdm8l0b1xbk506lbxjbch32y08; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY bien
    ADD CONSTRAINT fkdm8l0b1xbk506lbxjbch32y08 FOREIGN KEY (donacion_id) REFERENCES donacion(id);


--
-- Name: necesidad fkdnb4nk2ruvpp3y8tbaskgga0o; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY necesidad
    ADD CONSTRAINT fkdnb4nk2ruvpp3y8tbaskgga0o FOREIGN KEY (entidad_id) REFERENCES entidadbeneficiaria(id);


--
-- Name: donacion_fotos fkdnkg4b00wdmgtjah0ddjn2omv; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donacion_fotos
    ADD CONSTRAINT fkdnkg4b00wdmgtjah0ddjn2omv FOREIGN KEY (donacion_id) REFERENCES donacion(id);


--
-- Name: propuesta_candidatos fkdrcet7e4uqf4t35el7fxml3nl; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuesta_candidatos
    ADD CONSTRAINT fkdrcet7e4uqf4t35el7fxml3nl FOREIGN KEY (propuesta_id) REFERENCES propuestadeasignacion(id);


--
-- Name: personajejuridica fkf7ppfk5dw1q2a5mkl9e02ymg7; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY personajejuridica
    ADD CONSTRAINT fkf7ppfk5dw1q2a5mkl9e02ymg7 FOREIGN KEY (id) REFERENCES donante(id);


--
-- Name: donacion fkfdg5ow6yujkdymf99qirms7i; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donacion
    ADD CONSTRAINT fkfdg5ow6yujkdymf99qirms7i FOREIGN KEY (donante_id) REFERENCES donante(id);


--
-- Name: bien fkfjoo1kml1ue9db8l80tjxqo3w; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY bien
    ADD CONSTRAINT fkfjoo1kml1ue9db8l80tjxqo3w FOREIGN KEY (subcategoria_id) REFERENCES subcategoria(id);


--
-- Name: representante fkjert23eypye9bx1o52n9pmlkl; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY representante
    ADD CONSTRAINT fkjert23eypye9bx1o52n9pmlkl FOREIGN KEY (donante_id) REFERENCES personajejuridica(id);


--
-- Name: donante fkkb3n6p7jyoypxskbdjo89guyd; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY donante
    ADD CONSTRAINT fkkb3n6p7jyoypxskbdjo89guyd FOREIGN KEY (contacto_predeterminado_id) REFERENCES mediodecontacto(id);


--
-- Name: propuesta_candidatos fklv0d0oqsqggtp0awomu19v00r; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY propuesta_candidatos
    ADD CONSTRAINT fklv0d0oqsqggtp0awomu19v00r FOREIGN KEY (entidad_id) REFERENCES entidadbeneficiaria(id);


--
-- Name: usuario_roles fkuu9tea04xb29m2km5lwe46ua; Type: FK CONSTRAINT; Schema: donaciones; Owner: -
--

ALTER TABLE ONLY usuario_roles
    ADD CONSTRAINT fkuu9tea04xb29m2km5lwe46ua FOREIGN KEY (usuario_id) REFERENCES usuarios(id);


--
-- PostgreSQL database dump complete
--



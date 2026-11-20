--
-- PostgreSQL database cluster dump
--

\restrict uMOcsuGqX0o6dzo7qaHfSSPTXO6WGaETeuIM2BQeO0rN2J2UNvmdC7bxu8mEB0N

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE chamba_admin;
ALTER ROLE chamba_admin WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION NOBYPASSRLS;
CREATE ROLE chamba_api;
ALTER ROLE chamba_api WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION NOBYPASSRLS;
CREATE ROLE chamba_auth;
ALTER ROLE chamba_auth WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION NOBYPASSRLS;
CREATE ROLE chamba_billing;
ALTER ROLE chamba_billing WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION NOBYPASSRLS;
CREATE ROLE chamba_http_admin;
ALTER ROLE chamba_http_admin WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:LpnqJ0tJQW3non7QvW4Mwg==$GQYGRPc3BrCGigyWUezYR9EmN5hvjEr7iBmwyuTJv4k=:xtWZBPtj+5hB5JAzOo0PMzu8+61Stof3tEWCaHvQJFI=';
CREATE ROLE chamba_http_api;
ALTER ROLE chamba_http_api WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:2jiFqaxZDGRZqvpKTuAMiQ==$Ozs6CuiYyrGyyBmWiVXMIEwrMP0UfetHN6D6yGwYQkA=:OwlMY6KD1P8dula4EG7Tm5A7J4FATuunPH5GPxvTgkI=';
CREATE ROLE chamba_http_auth;
ALTER ROLE chamba_http_auth WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:yglyZEp7BMtfoEGnAlbuqw==$/BKyUQ1n5K9HeotlBQfnOMx1S/PqFkuVbX/QzEit/HY=:RKqu2lXcynB5RQUrtLPDW+eh0RNHTDOBuh2uCUGZIwo=';
CREATE ROLE chamba_http_billing;
ALTER ROLE chamba_http_billing WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:bdL/kdcBOn/ne+Se2pkeoA==$GHQ8ZdJe5D4Rxh6SDBSGkEOi5TDtRAIwnELwtk8I0r4=:w+jldoehqmoD34+gf81sJ2VzRu7UL7PEPnpGOBn1O38=';
CREATE ROLE chamba_http_worker;
ALTER ROLE chamba_http_worker WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:9KxxGUTI2vs9BN8GLC/zZg==$mJg0wmxb+tgf/W43wKl3NUzUKOBKBCcR2xLOMDkyKv4=:LTvxBL9HdosehX0jDWNr3owBbhtmBCu0KH7PNsNoBxg=';
CREATE ROLE chamba_owner;
ALTER ROLE chamba_owner WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:6ykv1hDe4+PwAcLhV8GA2A==$wZlt2XZznntO0lat/6P3Wj7qhN3YFgEZthZJ2jL4L+E=:uK1R7LUShI7uy/V+nzmLAMYlnsjKzHtk3+izSbCFp4s=';
CREATE ROLE chamba_worker;
ALTER ROLE chamba_worker WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB NOLOGIN NOREPLICATION NOBYPASSRLS;

--
-- User Configurations
--

--
-- User Config "chamba_http_admin"
--

ALTER ROLE chamba_http_admin SET statement_timeout TO '10s';
ALTER ROLE chamba_http_admin SET idle_in_transaction_session_timeout TO '15s';

--
-- User Config "chamba_http_api"
--

ALTER ROLE chamba_http_api SET statement_timeout TO '10s';
ALTER ROLE chamba_http_api SET idle_in_transaction_session_timeout TO '15s';

--
-- User Config "chamba_http_auth"
--

ALTER ROLE chamba_http_auth SET statement_timeout TO '10s';
ALTER ROLE chamba_http_auth SET idle_in_transaction_session_timeout TO '15s';

--
-- User Config "chamba_http_billing"
--

ALTER ROLE chamba_http_billing SET statement_timeout TO '10s';
ALTER ROLE chamba_http_billing SET idle_in_transaction_session_timeout TO '15s';

--
-- User Config "chamba_http_worker"
--

ALTER ROLE chamba_http_worker SET statement_timeout TO '10s';
ALTER ROLE chamba_http_worker SET idle_in_transaction_session_timeout TO '15s';


--
-- Role memberships
--

GRANT chamba_admin TO chamba_http_admin WITH INHERIT TRUE GRANTED BY chamba_owner;
GRANT chamba_api TO chamba_http_api WITH INHERIT TRUE GRANTED BY chamba_owner;
GRANT chamba_auth TO chamba_http_auth WITH INHERIT TRUE GRANTED BY chamba_owner;
GRANT chamba_billing TO chamba_http_billing WITH INHERIT TRUE GRANTED BY chamba_owner;
GRANT chamba_worker TO chamba_http_worker WITH INHERIT TRUE GRANTED BY chamba_owner;




\unrestrict uMOcsuGqX0o6dzo7qaHfSSPTXO6WGaETeuIM2BQeO0rN2J2UNvmdC7bxu8mEB0N

--
-- PostgreSQL database cluster dump complete
--


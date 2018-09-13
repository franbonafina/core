--ALTER SEQUENCE novedadparam_id_sequence RESTART WITH 3;

delete from CONCEPTO;

insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('50', 'Horas al 50%','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('100', 'Horas al 100%','ConceptoImpl');

insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('2004','Domingo','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('2520','Horas Extras','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('2650','Feriados','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('3198','Premio','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('2405','Plus por sabado y domingo','ConceptoImpl');
insert into CONCEPTO (ID,DESCRIPCION,TYPE) values ('6912','Almuerzo','ConceptoImpl');


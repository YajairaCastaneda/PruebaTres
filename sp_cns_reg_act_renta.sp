USE cotizacli
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_renta FROM mantencion
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_renta FROM ejecucion
go
IF OBJECT_ID('dbo.sp_cns_reg_act_renta') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_cns_reg_act_renta
    IF OBJECT_ID('dbo.sp_cns_reg_act_renta') IS NOT NULL
        PRINT '<<< FAILED DROPPING PROCEDURE dbo.sp_cns_reg_act_renta >>>'
    ELSE
        PRINT '<<< DROPPED PROCEDURE dbo.sp_cns_reg_act_renta >>>'
END
go
/****************************************************************************************/
/* Nombre SP                : sp_cns_reg_act_renta                                      */
/* Nombre BD                : cotizacli                                                 */
/* Tipo de ejecucion        : On Line                                                   */
/* Fecha creacion           : 14/07/2016                                                */
/* Autor                    : Ariel Acuna (SEnTRA) - Sergio Bravo (Ing. Soft BCI)       */
/* Objetivos                : Devuelve Datos Tabla ACT_RENTA                            */
/* Canal de ejecucion       : WEB/Internet                                              */
/* Parametros entrada       :   fec_ini   -   Fecha inicio,                             */
/* Retorno                  : no aplica                                                 */
/* Ejemplo de ejecucion     :   sp_cns_reg_act_renta  '14/07/2016'                      */
/****************************************************************************************/
CREATE PROC dbo.sp_cns_reg_act_renta(
        @fec_ini          DATETIME
)
as
BEGIN
    
        select act_rut, act_dv, act_id_canal, convert( char(8), act_fecha, 3 ) as fecha
        ,convert( char(8), act_fecha, 8 ) as hora, act_respuesta
        from ACT_RENTA
        where act_fecha>=@fec_ini
	
    
 
END
go
EXEC sp_procxmode 'dbo.sp_cns_reg_act_renta', 'unchained'
go
IF OBJECT_ID('dbo.sp_cns_reg_act_renta') IS NOT NULL
    PRINT '<<< CREATED PROCEDURE dbo.sp_cns_reg_act_renta >>>'
ELSE
    PRINT '<<< FAILED CREATING PROCEDURE dbo.sp_cns_reg_act_renta >>>'
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_renta TO ejecucion
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_renta TO mantencion
go

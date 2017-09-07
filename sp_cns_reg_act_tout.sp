USE cotizacli
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_tout FROM mantencion
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_tout FROM ejecucion
go
IF OBJECT_ID('dbo.sp_cns_reg_act_tout') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_cns_reg_act_tout
    IF OBJECT_ID('dbo.sp_cns_reg_act_tout') IS NOT NULL
        PRINT '<<< FAILED DROPPING PROCEDURE dbo.sp_cns_reg_act_tout >>>'
    ELSE
        PRINT '<<< DROPPED PROCEDURE dbo.sp_cns_reg_act_tout >>>'
END
go
/****************************************************************************************/
/* Nombre SP                : sp_cns_reg_act_tout                                       */
/* Nombre BD                : cotizacli                                                 */
/* Tipo de ejecucion        : On Line                                                   */
/* Fecha creacion           : 14/07/2016                                                */
/* Autor                    : Ariel Acuna (SEnTRA) - Sergio Bravo (Ing. Soft BCI)       */
/* Objetivos                : Buscar registros con error 0008 Tabla ACT_RENTA           */
/* Canal de ejecucion       : WEB/Internet                                              */
/* Parametros entrada       : @fec_ini con la fecha desde donde se debe considerar      */
/* Retorno                  : Lista de registros.                                       */
/* Ejemplo de ejecucion     : sp_cns_reg_act_tout '07/07/2016'                          */
/****************************************************************************************/
CREATE PROC dbo.sp_cns_reg_act_tout(
        @fec_ini          DATETIME
)
as
BEGIN
    declare @max_intentos integer
    
    select @max_intentos = CAST( par_valor AS INTEGER)
       from PARAMETROS 
       where par_id = "ITER_MAX"
        
    select act_rut, act_dv, act_id_canal, act_id_campana, act_atm, convert( char(8), act_fecha, 3 ) as fecha
        ,convert( char(8), act_fecha, 8 ) as hora, act_cod_error, act_glosa_error
       from ACT_RENTA
       where act_cod_error='0008' and act_iteracion>=@max_intentos and act_fecha>=@fec_ini
	
    
 
END
go
EXEC sp_procxmode 'dbo.sp_cns_reg_act_tout', 'unchained'
go
IF OBJECT_ID('dbo.sp_cns_reg_act_tout') IS NOT NULL
    PRINT '<<< CREATED PROCEDURE dbo.sp_cns_reg_act_tout >>>'
ELSE
    PRINT '<<< FAILED CREATING PROCEDURE dbo.sp_cns_reg_act_tout >>>'
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_tout TO ejecucion
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_tout TO mantencion
go

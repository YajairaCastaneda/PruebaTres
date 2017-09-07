USE cotizacli
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_reint FROM mantencion
go
REVOKE EXECUTE ON dbo.sp_cns_reg_act_reint FROM ejecucion
go
IF OBJECT_ID('dbo.sp_cns_reg_act_reint') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_cns_reg_act_reint
    IF OBJECT_ID('dbo.sp_cns_reg_act_reint') IS NOT NULL
        PRINT '<<< FAILED DROPPING PROCEDURE dbo.sp_cns_reg_act_reint >>>'
    ELSE
        PRINT '<<< DROPPED PROCEDURE dbo.sp_cns_reg_act_reint >>>'
END
go
/*****************************************************************************************/
/* Nombre SP                : sp_cns_reg_act_reint                                       */
/* Nombre BD                : cotizacli                                                  */
/* Tipo de ejecucion        : On Line                                                    */
/* Fecha creacion           : 01/08/2016                                                 */
/* Autor                    : Felipe Briones M. (SEnTRA) - Sergio Bravo (Ing. Soft. BCI) */
/* Objetivos                : Buscar registros con error 0008 los cuales seran           */
/*         reprocesados por una shell si es que no estan sobre el limite de iteraciones. */
/* Canal de ejecucion       :  web                                                       */
/* Parametros entrada       : No aplica                                                  */
/* Retorno                  : Rut, dv e iteracion actual.                                */
/* Ejemplo de ejecucion     : sp_cns_reg_act_reint                                       */
/*****************************************************************************************/
CREATE PROC dbo.sp_cns_reg_act_reint
as
BEGIN
    declare @max_intentos integer

    select @max_intentos = convert(integer, par_valor)
    from PARAMETROS 
    where par_id = "ITER_MAX"

    select act_rut, 
           act_dv,
           act_iteracion
    from ACT_RENTA
    where act_cod_error='0008' and act_iteracion <= @max_intentos

END
go
EXEC sp_procxmode 'dbo.sp_cns_reg_act_reint', 'unchained'
go
IF OBJECT_ID('dbo.sp_cns_reg_act_reint') IS NOT NULL
    PRINT '<<< CREATED PROCEDURE dbo.sp_cns_reg_act_reint >>>'
ELSE
    PRINT '<<< FAILED CREATING PROCEDURE dbo.sp_cns_reg_act_reint >>>'
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_reint TO ejecucion
go
GRANT EXECUTE ON dbo.sp_cns_reg_act_reint TO mantencion
go

USE cotizacli
go
REVOKE EXECUTE ON dbo.sp_act_reg_act_renta FROM mantencion
go
REVOKE EXECUTE ON dbo.sp_act_reg_act_renta FROM ejecucion
go
IF OBJECT_ID('dbo.sp_act_reg_act_renta') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_act_reg_act_renta
    IF OBJECT_ID('dbo.sp_act_reg_act_renta') IS NOT NULL
        PRINT '<<< FAILED DROPPING PROCEDURE dbo.sp_act_reg_act_renta >>>'
    ELSE
        PRINT '<<< DROPPED PROCEDURE dbo.sp_act_reg_act_renta >>>'
END
go
/****************************************************************************************/
/* Nombre SP                : sp_act_reg_act_renta                                      */
/* Nombre BD                : cotizacli                                                 */
/* Tipo de ejecucion        : On Line                                                   */
/* Fecha creacion           : 14/07/2016                                                */
/* Autor                    : Ariel Acuna (SEnTRA) - Sergio Bravo (Ing. Soft BCI)       */
/* Objetivos                : Actualizar Datos Tabla ACT_RENTA                          */
/* Canal de ejecucion       : WEB/Internet                                              */
/* Parametros entrada       :   act_rut         -   Rut a encontrar o crear,            */
/*                                act_dv          -   Codigo verificador,               */
/*                                act_iteracion   -   Iteracion,                        */
/*                                act_cod_error   -   Codigo de error,                  */
/*                                act_glosa_error -   Glosa Error                       */
/*                                                                                      */
/* Retorno                  : no aplica                                                 */
/* Ejemplo de ejecucion     :   sp_act_reg_act_renta 10050353,'0',1,'3008', 'Error'     */
/****************************************************************************************/
CREATE PROC dbo.sp_act_reg_act_renta(
        @act_rut                    int,
        @act_dv             VARCHAR(1),
        @act_iteracion      INT,
        @act_cod_error      VARCHAR(5),
        @act_glosa_error    VARCHAR(100)
        
)
as
BEGIN
    declare @xRut integer
    select @xRut=(select count(act_rut)
	from ACT_RENTA
	where act_rut=@act_rut)
    
    if (@xRut!=0)
    begin
		update ACT_RENTA
		set act_iteracion=@act_iteracion, act_cod_error=@act_cod_error, act_glosa_error=@act_glosa_error  
		where act_rut=@act_rut and act_dv=@act_dv
		if(@@error !=0)
		begin
		  select 0
		end
        else
          select 1
	end

    

END
go
EXEC sp_procxmode 'dbo.sp_act_reg_act_renta', 'unchained'
go
IF OBJECT_ID('dbo.sp_act_reg_act_renta') IS NOT NULL
    PRINT '<<< CREATED PROCEDURE dbo.sp_act_reg_act_renta >>>'
ELSE
    PRINT '<<< FAILED CREATING PROCEDURE dbo.sp_act_reg_act_renta >>>'
go
GRANT EXECUTE ON dbo.sp_act_reg_act_renta TO ejecucion
go
GRANT EXECUTE ON dbo.sp_act_reg_act_renta TO mantencion
go

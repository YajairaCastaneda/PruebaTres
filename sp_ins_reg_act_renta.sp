USE cotizacli
go
REVOKE EXECUTE ON dbo.sp_ins_reg_act_renta FROM mantencion
go
REVOKE EXECUTE ON dbo.sp_ins_reg_act_renta FROM ejecucion
go
IF OBJECT_ID('dbo.sp_ins_reg_act_renta') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_ins_reg_act_renta
    IF OBJECT_ID('dbo.sp_ins_reg_act_renta') IS NOT NULL
        PRINT '<<< FAILED DROPPING PROCEDURE dbo.sp_ins_reg_act_renta >>>'
    ELSE
        PRINT '<<< DROPPED PROCEDURE dbo.sp_ins_reg_act_renta >>>'
END
go
/****************************************************************************************/
/* Nombre SP                : sp_ins_reg_act_renta                                      */
/* Nombre BD                : cotizacli                                                 */
/* Tipo de ejecucion        : On Line                                                   */
/* Fecha creacion           : 13/07/2016                                                */
/* Autor                    : Ariel Acuna (SEnTRA) - Sergio Bravo (Ing. Soft BCI)       */
/* Objetivos                : Insertar o Actualizar Datos Tabla ACT_RENTA               */
/* Canal de ejecucion       : WEB/Internet                                              */
/* Parametros entrada       :   act_rut         -   Rut a encontrar o crear,            */
/*                              act_dv          -   Codigo verificador,                 */
/*                              act_atm         -   Id del cajero ATM,                  */
/*                              act_id_canal    -   Id de Canal,                        */
/*                              act_id_campana  -   Id de Campana,                      */
/*                              act_iteracion   -   Iteracion,                          */
/*                              act_respuesta   -   Respuesta,                          */ 
/*                              act_cod_error   -   Codigo de error,                    */
/*                              act_glosa_error -   Glosa Error                         */
/* Retorno                  : valor 0 en caos de error, valor 1 en caso OK              */
/* Ejemplo de ejecucion     :                                                           */
/* sp_ins_reg_act_renta 8910091, '7', '04553', '100', 1000006, 6, 'N', '0003', 'TIME OUT'*/
/****************************************************************************************/
CREATE PROC dbo.sp_ins_reg_act_renta(
        @act_rut                    int,
        @act_dv             VARCHAR(1),
        @act_atm            VARCHAR(20),
        @act_id_canal       VARCHAR(3),
        @act_id_campana     INT,
        @act_iteracion      INT,
        @act_respuesta      VARCHAR(1),
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
		set act_atm=@act_atm, act_fecha=getdate(), act_id_canal=@act_id_canal
        , act_id_campana=@act_id_campana, act_iteracion=@act_iteracion, act_respuesta=@act_respuesta
		, act_cod_error=@act_cod_error ,act_glosa_error=@act_glosa_error
		where act_rut=@act_rut  and act_dv=@act_dv
		if(@@error !=0)
		begin
		  select 0
		end	    
	end
    else
	begin
		insert into ACT_RENTA
		(act_rut, act_dv, act_atm, act_fecha, act_id_canal, act_id_campana
		, act_iteracion, act_respuesta, act_cod_error, act_glosa_error)
		values
		(@act_rut, @act_dv, @act_atm, getdate(), @act_id_canal, @act_id_campana, @act_iteracion
        , @act_respuesta, @act_cod_error, @act_glosa_error)
		if(@@error !=0)
		begin
		  select 0
		end
	end
    
    select 1
END
go
EXEC sp_procxmode 'dbo.sp_ins_reg_act_renta', 'unchained'
go
IF OBJECT_ID('dbo.sp_ins_reg_act_renta') IS NOT NULL
    PRINT '<<< CREATED PROCEDURE dbo.sp_ins_reg_act_renta >>>'
ELSE
    PRINT '<<< FAILED CREATING PROCEDURE dbo.sp_ins_reg_act_renta >>>'
go
GRANT EXECUTE ON dbo.sp_ins_reg_act_renta TO ejecucion
go
GRANT EXECUTE ON dbo.sp_ins_reg_act_renta TO mantencion
go

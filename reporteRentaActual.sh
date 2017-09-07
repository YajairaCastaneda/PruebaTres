#!/usr/bin/sh
#************************************************************************	
# Nombre      : reporteRentaActual.sh
# Descripcion : Proceso encargado de generar archivo con reporte de rentas actualizadas
#               para ser enviado via email.
# Autor	      : Andres Moran Ortiz
# Empresa     : SEnTRA
# Sintaxis    : sh reporteRentaActual.sh
# Fecha	      : 28-07-2016
#************************************************************************
# Mantencion : 
#************************************************************************

#---------------------------------------------------------------------
#  Funciones de apoyo ( Definicion de funciones globales a utilizar )
#---------------------------------------------------------------------

printlog()
{
   Hora=`date +%H:%M:%S`
   echo ${Hora} "$@" >> ${LOGFILE}
}

SALIR()
{
   printlog "*********************************************************"
   printlog "**                 RESUMEN                             **"
   printlog "*********************************************************"
   printlog "Servidor    : `uname -n`"
   printlog "Usuario     : `whoami`"
   printlog "Ambiente    : `BciAmbiente`"
   printlog "Programa    : ${INF_PGM}"
   printlog "ID proceso  : ${INF_PID}"
   printlog "Hora Inicio : ${INF_INI}"
   printlog "Hora Termino: `date +%d/%m/%Y\" \"%H:%M:%S`"
   printlog "Parametros  : ${INF_ARG}"
   printlog "Resultado   : `CTM_Error E$1` ($1)"
   printlog "Archivo Log : ${LOGFILE}"
   printlog "*********************************************************"
   cat ${LOGFILE}
   exit ${1}
}

VerificaEjecucion()
{
	if [[ $? != 0 ]]; then
		printlog "* ERROR : La ejecucion SQL(${sp_proc}) se cancelo"
		echo "* ERROR : La ejecucion SQL(${sp_proc}) se cancelo"
		if [ -f ${OUTFILE} ]; then
			printlog "------------------------------------------------------"
			printlog `tail -5 ${OUTFILE}`
			printlog "------------------------------------------------------"
		fi
		printlog "* ERROR : se aborta ejecucion"
		SALIR 2
	elif [ -f ${OUTFILE} ]; then 
		estadoRetorno=`egrep '^Msg [0-9]*, Level [0-9]*, State' ${OUTFILE}`
		if [[ "${estadoRetorno}" != "" ]]; then
			printlog "- ERROR : El comando SQL termino con error"
			printlog "------------------------------------------------------"
			printlog `tail  ${OUTFILE}`
			printlog "------------------------------------------------------"
			echo "* ERROR : El comando SQL termino con error"
			SALIR 2
		else
			printlog "- Correcta ejecucion del ${1}."
		fi
	else
		printlog "* ERROR : No se creo el archivo de salida"
		printlog "* ERROR : Se aborta ejecucion"
		SALIR 2
	fi
}

setuservar()
{
	UYPRSP=`UyPuser $1`
	if [ $? != 0 ] ; then
		printlog "ERROR : Se cancelo la ejecucion de UyPuser"
		printlog "ERROR : SISTEMA = $1 "
		printlog "ERROR : RETORNO = ${UYPRSP}"
		P_STAT="ERR"
	else
		uypcnt=0
		for uyprsp in ${UYPRSP}
		do
			uypDa[$uypcnt]=$uyprsp
			(( uypcnt=$uypcnt+1))
		done
		if [ "${uypDa[0]}" = "ERROR" ] ; then
			printlog "ERROR : No se pudo obtener la clave del sistema $1"
			printlog "ERROR : RETORNO = ${uypDa[1]}"
			printlog "ERROR : se aborta ejecucion"
			SALIR 2
		else
			P_USUARIO=${uypDa[0]}
			P_CLAVE=${uypDa[1]}
			P_STAT="OK"
		fi
	fi
	printlog "- Obtencion de clave para sistema ( $1 ) ${P_STAT}"
}

#----------------------------------------------------------------------------#
#                       Carga Informacion de ejecucion                       #
#----------------------------------------------------------------------------#
INF_PGM=`echo $0 |sed s/"\/"/" "/g|awk '{print $(NF)}'`
BSE_PGM=`echo ${INF_PGM} |cut -d"." -f1`
INF_PID=$$
INF_ARG=$@
INF_STT=0
INF_INI=`date +%d/%m/%Y" "%H:%M:%S`

#---------------------------------------------------------------------
#  Definicion raiz de directorios ($HOME) y archivo de configuracion 
#---------------------------------------------------------------------
BaseDir="$HOME"
ENVFILE="${BaseDir}/cfg/profileReporteRenta"
   
#---------------------------------------------------------------------
#  Definicion de valiables locales
#---------------------------------------------------------------------
P_USUARIO=""
P_CLAVE=""
P_STAT=OK
fecPro=`date +%Y%m%d`
DiaEje=`date +%Y%m%d`
OUTFILE="${BaseDir}/tmp/${BSE_PGM}.${INF_PID}.out"
LOGFILE="${BaseDir}/log/${fecPro}.${BSE_PGM}.log"
DIRLOG=${BaseDir}/log
DIRTMP=${BaseDir}/tmp
DIRBKP=${BaseDir}/bkp
DIRDAT=${BaseDir}/dat
archivoCorreo=${BSE_PGM}.email
cmdFtp=${DIRTMP}/${BSE_PGM}.ftp
ftplog=${DIRTMP}/${BSE_PGM}.logFTP
NombreInterfazRenta="CLI_ATM-"${fecPro}".CSV"

#---------------------------------------------------------------------
#  Definicion de ambiente de ejecucion
#---------------------------------------------------------------------
if [ "${ENVFILE}" != "" ]; then
	if [ ! -x ${ENVFILE} ]; then
		printlog "ERROR : No existe el archivo ${ENVFILE}"
        printlog "ERROR : no se pudo configurar el ambiente"
        printlog "ERROR : se aborta ejecucion"
        SALIR 2
    fi
    . ${ENVFILE}
    if [ $? != 0 ]; then
		printlog "ERROR : Se cancelo la ejecucion de ${ENVFILE}"
        printlog "ERROR : no se pudo configurar el ambiente"
        printlog "ERROR : se aborta ejecucion"
        SALIR 2
    fi
    printlog "- Ambiente configurado OK"
else
	printlog "- Sin configuracion externa"
fi

#--------------------------------------------------------
# Obtencion de clave del sistema CTASTER
#--------------------------------------------------------
setuservar ${SISTEMA_COTIZA}
if [ "${P_STAT}" = "OK" ]; then
   P_USER_COTIZA=${P_USUARIO}
   P_CLAVE_COTIZA=${P_CLAVE}
fi

#obtenemos la ultima fecha de ejecucion

fechaUltimaEjecucion=`date +%Y-%m-%d" "%H:%M:%S`

if [ -f ${DIRDAT}/${ARCH_ULT_EJE} ]; then
   fechaEjecucionAnterior=`cat ${DIRDAT}/${ARCH_ULT_EJE}`
else
   fechaEjecucionAnterior=${fechaUltimaEjecucion}
fi

echo ${fechaUltimaEjecucion} > ${DIRDAT}/${ARCH_ULT_EJE}

#Rescatamos los registros para la fecha 
printlog "- exec sp_cns_reg_act_renta ${fechaEjecucionAnterior}"
	
isql -U${P_USER_COTIZA} -S${DSQUERY_COTIZA} -w10000 -s";" -Jiso_1 << EOF > ${OUTFILE}
`echo ${P_CLAVE_COTIZA}`
use ${BD_COTIZA}
go
set nocount on
go
exec sp_cns_reg_act_renta "${fechaEjecucionAnterior}"
go
EOF
VerificaEjecucion "sp_cns_reg_act_renta"

cat ${OUTFILE} | tail +4 | awk 'length($0)>0'| grep -v "return status" | awk '{ print substr($0,2,length($0))}' | sed 's/.$//g' > ${DIRTMP}/${NombreInterfazRenta}

CANTREG=0
CANTREG=`wc -l ${DIRTMP}/${NombreInterfazRenta} | cut -f1 -d " " | awk '{printf ("%d", $0);}`

if [ $CANTREG -lt 1 ]; then
   printlog "- No se encontraron actualizaciones para el dia ${fechaEjecucionAnterior}"
   rm -f ${OUTFILE} 2>/dev/null
   rm -f ${DIRTMP}/${NombreInterfazRenta} 2>/dev/null
   SALIR 4
fi

rm ${OUTFILE} 2>/dev/null



printlog "- Preparando envio a ${SERVER_CORREO}"

echo "own: ${MAIL_OWN}" > ${DIRTMP}/${archivoCorreo}

i=0
for dest in ${DEST_CORREO}
do
  echo "usr: ${dest}" >> ${DIRTMP}/${archivoCorreo}
done

echo "ath: ${NombreInterfazRenta}"     >> ${DIRTMP}/${archivoCorreo}
echo "sbj: ${ASUNTO_CORREO}" >> ${DIRTMP}/${archivoCorreo}
echo "msg:"                               >> ${DIRTMP}/${archivoCorreo}
echo "${MSG_CORREO}"                >> ${DIRTMP}/${archivoCorreo}
echo "${FIRMA_CORREO}"                >> ${DIRTMP}/${archivoCorreo}

#instrucciones ftp

echo "asc"                        > $cmdFtp
if [ "${DIR_ATTACH}" != "" ]; then
   echo "cd ${DIR_ATTACH}" >> $cmdFtp
fi

echo "lcd ${DIRTMP}"       >> $cmdFtp
echo "put ${NombreInterfazRenta}"       >> $cmdFtp
echo "cd .." >> $cmdFtp
echo "put ${archivoCorreo}" >> $cmdFtp
echo "bye" >> $cmdFtp

ftp -v ${SERVER_CORREO} < ${cmdFtp} > ${ftplog}
V_FTP=`grep 226 $ftplog|wc|awk '{printf $1 }'`
if [ $V_FTP -gt 0 ]; then 
   printlog "- Exito al realizar el ftp del archivo"
else
   printlog "- Error al realizar el ftp del archivo ${NombreInterfazRenta}"
   printlog `tail  ${ftplog}`
   SALIR 2
fi

rm -f ${DIRTMP}/${archivoCorreo}
rm -f ${DIRTMP}/${NombreInterfazRenta}
rm -f ${ftplog}
rm -f ${cmdFtp}
rm -f ${OUTFILE}

SALIR 0

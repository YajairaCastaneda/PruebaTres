#!/usr/bin/sh
#*************************************************************************************
# Nombre      : reintentoActualizaRenta.sh
# Descripcion : Reenvia a los clientes con error para reintentar actualizar su renta.
# Autor	      : Felipe Briones M. (SEnTRA) - Sergio Bravo (Ing. Soft. BCI)
# Empresa     : SEnTRA
# Sintaxis    : sh reintentoActualizaRenta.sh
# Fecha	      : 01-08-2016
#*************************************************************************************
# Mantencion : 
#*************************************************************************************

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
BaseDir="$HOME/reintentoActualizaRenta"
ENVFILE="${BaseDir}/cfg/profile"
   
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
DIRLIB=${BaseDir}/lib
DIRBIN=${BaseDir}/bin
DIRCFG=${BaseDir}/cfg

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

#Rescatamos los registros para la fecha 
printlog "- exec sp_cns_reg_act_renta "
	
isql -U${P_USER_COTIZA} -S${DSQUERY_COTIZA} -w10000 -s";" -Jiso_1 << EOF > ${OUTFILE}
`echo ${P_CLAVE_COTIZA}`
use ${BD_COTIZA}
go
set nocount on
go
exec sp_cns_reg_act_reint 
go
EOF

VerificaEjecucion "sp_cns_reg_act_renta"

cat ${OUTFILE} | tail +4 | awk 'length($0)>0'| grep -v "return status" | awk '{ print substr($0,2,length($0))}' | sed 's/.$//g' > ${OUTFILE}

CANTREG=0
CANTREG=`wc -l ${OUTFILE} | cut -f1 -d " " | awk '{printf ("%d", $0);}`

if [ $CANTREG -lt 1 ]; then
   printlog "- No se encontraron registros"
   rm -f ${OUTFILE} 2>/dev/null
   SALIR 4
fi

#ClassPath para ejecutar java
PATH_SEPARATOR=":"
CP=$DIRLIB/javax.jms_1.1.1.jar
CP=$CP$PATH_SEPARATOR$DIRLIB/wlclient.jar
CP=$CP$PATH_SEPARATOR$DIRLIB/log4j-1.2.8.jar
CP=$CP$PATH_SEPARATOR$DIRBIN/ReintentoActualizacionRenta.jar
CP=$CP$PATH_SEPARATOR$DIRCFG
MAIN_CLASS="cl.bci.aplicaciones.renta.ReintentoActualizacionRenta"

printlog "- Inicia proceso de reintento renta JMS"
printlog "* Cantidad Registros a Procesar: $CANTREG"
#itera
RESULTADO=0
while read lineaArchivo           
	do               
		rut=`echo $lineaArchivo | awk -F  ";" '{ printf($1)}'`
		dv=`echo $lineaArchivo | awk -F  ";" '{ printf($2)}'`
		iteracion=`echo $lineaArchivo | awk -F  ";" '{ printf($3)}'`
		printlog "$JAVA_HOME/bin/java -classpath $CP $MAIN_CLASS $FACTORY_JMS $QUEUE_JMS $IP_SERVER_JMS $FACTORY_CLASS $rut $dv $iteracion"
		#Ejecucion de jar
		RESULTADO=`$JAVA_HOME/bin/java -classpath $CP $MAIN_CLASS $FACTORY_JMS $QUEUE_JMS $IP_SERVER_JMS $FACTORY_CLASS $rut $dv $iteracion`
		if [ $RESULTADO -gt 0 ]; then
		   printlog "- ERROR en la ejecucion Java, retorno $RESULTADO"
		   break
		fi
	done <  ${OUTFILE}

rm -f ${OUTFILE}

SALIR $RESULTADO

#!/bin/bash 

BITOOPIPE=/var/run/bitoo/input

TEMPDIR=`mktemp -d`

TEMPPIPE=$TEMPDIR/input

mkfifo $TEMPPIPE

{
	echo $TEMPPIPE
	echo $1
} > $BITOOPIPE

{
	read BEGIN
} < $TEMPPIPE


if [ $BEGIN != "BEGIN" ]
then
	echo "Comunication with BiToo daemon failed"
	EXITVAL=1
else

	{
		read STATUS
	} < $TEMPPIPE

	if [ $STATUS == "OK" ]
	then
        	echo "Download completed"
		EXITVAL=0
	else
		echo "Download failed"
		EXITVAL=2
	fi
fi	

rm $TEMPPIPE
rmdir $TEMPDIR

if [ $EXITVAL -ne 0 ]
then
	source /etc/make.globals
	source /etc/make.conf
	for MIRROR in $GENTOO_MIRRORS 
	do  
		echo
	done 
	URL=$MIRROR$1
	if [ -e /usr/bin/getdelta.sh ]
	then
		/usr/bin/getdelta.sh $URL
	else
		/usr/bin/wget -t 1--passive-ftp $URI -P $DISTDIR
	fi
else
	exit $EXITVAL
fi

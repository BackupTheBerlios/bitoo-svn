#!/bin/bash

FILES=`ls /usr/portage/distfiles/`
CLASSPATH=/home/abel/src/bitoo/dist/lib/BiToo-20041204.jar$CLASSPATH

#echo $FILES

for file in $FILES
do
	if [  -f "$file" ] 
	then
		echo "$file not a distfiles skipping"; echo
		continue
	fi

	pfile=/usr/portage/distfiles/$file

	dfile=distfiles/$file
	
	mlist="http://gentoo.inode.at/$dfile|http://ftp.belnet.be/mirror/rsync.gentoo.org/gentoo/$dfile|http://trumpetti.atm.tut.fi/gentoo/$dfile|http://ftp.uni-erlangen.de/pub/mirrors/gentoo/$dfile|http://ftp.easynet.nl/mirror/gentoo/$dfile|http://mirror.gentoo.no/$dfile|http://gentoo.spb.ru/rsync/$dfile|http://mirror.switch.ch/mirror/gentoo/$dfile|http://www.mirrorservice.org/sites/www.ibiblio.org/gentoo/$dfile"

	echo "Creating bitoorrent for $file..." 
	java info.bitoo.utils.BiToorrentMaker http://www.bitoo.info/tracker/announce.php $pfile --locations=$mlist --target=$file.torrent
	echo "Done"
done
exit 0

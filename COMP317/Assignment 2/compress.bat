@ECHO OFF

IF "%1"=="" GOTO BLANK_FILE_NAME

IF NOT EXIST %1 GOTO NOT_EXIST

type %1 | java Encoder | java Bitpacker > %1.lz78
GOTO END
	
:NOT_EXIST	
ECHO The specified file '%1' does not exist.
GOTO END

:BLANK_FILE_NAME
ECHO USAGE: compress filename [maxBits]

:END
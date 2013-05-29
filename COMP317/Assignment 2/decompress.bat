@ECHO OFF

IF "%1"=="" GOTO BLANK_FILE_NAME

IF NOT EXIST %1 GOTO NOT_EXIST

IF NOT "%~x1"==".lz78" GOTO BAD_EXTENSION

type %1 | java Bitunpacker | java Decoder > %~n1

GOTO END

:NOT_EXIST
ECHO The specified file '%1' does not exist.
GOTO END

:BLANK_FILE_NAME
ECHO You must provide a file name as an argument
GOTO END

:BAD_EXTENSION
ECHO You must provide a file with extension '.lz78'

:END

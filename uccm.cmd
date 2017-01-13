@echo off

rem 
rem  It's an uCcm starting script
rem 
rem  normally it needs to be redistributed as part of the your project
rem

rem  Environment variable UCCM100REPO says 
rem     where uccm should place all packages and itself also
rem  Environment variable UCCM100DEV says
rem     you are using development version taken from git repository
rem     and where it is

cd %~dp0
set PROGNAME=%~nx0

if "%UCCM100REPO%"=="" set UCCM100REPO=%LOCALAPPDATA%\uCcm100Repo

if not "%UCCM100DEV%"=="" (
	echo "** Development uCcm version is using **"
	call %UCCM100DEV%\uccm100.cmd %*
	if %errorlevel% NEQ 0 exit 1
	goto :eof
)

if "%*"=="--uccm-update" (
	if exist "%UCCM100REPO%\uccm-uccm100" rmdir /Q /S "%UCCM100REPO%\uccm-uccm100"
	call :update_uccm
	if %errorlevel% NEQ 0 exit 1
	goto :eof
)

if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" call :update_uccm
if %errorlevel% NEQ 0 exit 1
call "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*
if %errorlevel% NEQ 0 exit 1
exit /B 0

:update_uccm
java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :eof
echo "failed to acquire uCcm build manager"
exit 1




PK  �-J            	  META-INF/��   PK           PK  �-J               META-INF/MANIFEST.MF�M��LK-.�K-*��ϳR0�3��r.JM,IM�u�	X�ě�+h�%&�*8��%� �k�r�&f��:�$[)�&'�깧��i^.^. PKX�j�[   ^   PK  �-J               uccm/GetUccm.class�V�wE�M�f��m� )�¥(VM��PI�����6ڥ���B�x��"�����o)Z������?૾�/��X��$��cN�|3�|��w�����/����h����.eHT�=���c��^I��N��Ѱ��VI�V��H(�t�ԽM��r�wJ�]r�I�T0�@��^�W`��(��>cH)H�0a��q�2r�Q��pT����*��9�sL����{�8�Uq-�㸟��&�4�f����]�6kX0��S$������C)��Һa2,������єn�D���a�4I��>GO�u�'Oi�p<�����p<J9#H��A���LRl5�um�pv&��i[����x�S8ѝ�q�ب���X4:b8��PC�JG��YwXO�
3ꒅ��I����ذ~}�AÖ�xBÓx�a����FjXd(l�	�Y�g=la��gp��YG�8C�ζ�.2���ӭ�9<_DjX�B0�xKOO{K��cxAË��妆æ���p8e%�T�Ŷ�u��~	�ɼ�����%q���^`�I�����8���R�����+�J1u���:�����T��e�"�P�^��T%��ehD8r'<��S_Hh�Ĩ��������wd��]Ӧ�Kbz�]�HE	Ptа%�x�@�{2#��j�K$;L'3��C��;���](Y�xR؎a�>��īz��\���G��,-�0�����0��i-�*�^[!Q�Y�AJ�f�NI���=/��~VJ0�MdA&�-���E�=$\�OX��ms��~6���
�L��c�Yo�R����J<RO�r����
��ϰ���`Ÿ�u�ލ2H�������E:e�G���4{�*�cÆ<��e(k�\G���(CM�`:�_�����8���-ze�{�ƢT�ּ���K��,�������/%$�#J%���i]�I�@�p:�u����d��J��3`�`�h���"s��z��ʈ��H�IW�b�),/�w�,�'�	=Mn��l��!���`�FⳟŦ��Sk�;�N~FO
\���ɔ�20y��x#��eD+�L�}F��i�{L�h¦�h�o��vze��)�⟣b��)���C��T"9T��Ϣ�{̛F�7��I4x��u�>��I�~�y����;��ܯ,��®uޮBd��W!�A��������4���1_ ���(o��7r�\ER�nYL	��Jɼ����PE+�kX�*ʃ<��A���蟜�9��aU�E��jR���emLQ��bU��/P��)8$gt�~�hL�q�i9��Pb�r��懪���.�=�	:T���R�XeUcM��ByX5�_��̯&������Q|e?��c��K�)Q�U��� �N�\�DX�f,�ԡ�1�%�W��2�nY+��\EEp��MXC��Z���1E�_���?����s�j�ߩ���>��:f�Y%�Y�����]���,�v6�6��lۘ��� :�c��#N_T	v}����-�z����fB?��sh�h>O@9��P4�7q��T�`�޿��S������cG����̱�D�a	���3�T�.�ʱ]�vz'�� PK��R  r
  PK   �-J           	                META-INF/��  PK   �-JX�j�[   ^                =   META-INF/MANIFEST.MFPK   �-J��R  r
               �   uccm/GetUccm.classPK      �   l    
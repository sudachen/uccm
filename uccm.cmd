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

for %%i in (%*) do if %%i == --self-update goto :do_self_update
for %%i in (%*) do if %%i == --no-dev goto :exec_no_dev

if not "%UCCM100DEV%" == "" goto :exec_dev_version

:exec_no_dev
if not exist "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" call :update_uccm
if %errorlevel% NEQ 0 exit 1
call "%UCCM100REPO%\uccm-uccm100\uccm100.cmd" %*
if %errorlevel% EQU 0 goto :eof
exit 1

:do_self_update
if exist "%UCCM100REPO%\uccm-uccm100" rmdir /Q /S "%UCCM100REPO%\uccm-uccm100"
call :update_uccm
if %errorlevel% EQU 0 goto :eof
exit 1

:update_uccm
java -jar %PROGNAME%
if %errorlevel% EQU 0 goto :eof
echo "failed to acquire uCcm build manager"
exit 1

:exec_dev_version
echo *** Development uCcm version is using now ***
call %UCCM100DEV%\uccm100.cmd %*
if %errorlevel% EQU 0 goto :eof
exit 1

goto :eof
rem
rem
rem
rem
PK  km3J            	  META-INF/��   PK           PK  km3J               META-INF/MANIFEST.MF�M��LK-.�K-*��ϳR0�3��r.JM,IM�u�	X�ě�+h�%&�*8��%� �k�r�&f��:�$[)�&'�깧��i^.^. PKX�j�[   ^   PK  jm3J               uccm/GetUccm.class�V�sU��&��n�-P�KD��"�����B/`��mrh�ݘ�b����o ���ֻ�3)Z�Q�'�_|�W_g�w6�D�3f2����w����;{�����
���L��Hpt(�TQ��tcG�

z��'w��;�K�-*�p�h@���r�U�ަ�v�y��;�P�Ð���S
��=�qX#��E��;�d���L��,�R�C^�\�
��*Vc���(��pǽ*��>9�/��xP%�q<���o�a�FOx�No��	��NfH����4I��0�w'����HZ7�#�v�0�[�a]��'�u�YW�rGy�x��1��9��x�0r<E���k9���bH��Va�'��&�[�XO27�)�HO�!:b��|46�g�)ie"zZ�����ғ#8�!��#�~�.�^���#+�8��i<�0�R���N��6쐓�P����,���<q���E`��ok� �=��./��2RÊ��$��b�Xw��X_L�+8���0i9�Tȴ���L��VRO�b��f�&֯�(�w�\�="kI��4�a���.�{�x�)ըH�u��pR�p^%�.��:61zFz~����LS$m�ASS��7��J�)��4[�ۻKY�UR��x�J�a�jxOƪs�,u�4 %��۱�t�PE����}|@]��C����1��e�q�΍i��4|*�N�ì�u��&E�6,S�Y�W�җ��a�Eͮ��D����[���U&��jwkV���J���22j7+l*��M'#rzQ;���T`��ۂ\x,��e�ݤ\�OX}z6+�C�t/����*��o�rŖ
F���r�&>ja�gX��BK�b���y��2H�����z��2KÉ��l�>Mՙ})CM��[i�ݺ=�P�$����l��];���[�J��i��e�]t3/�%A�[����_��e�A�	�Zcn�bЊR��L��n]�N��9�&&���n���i+/%���!(����K[��5���+'�i�!Ү5Ũ]Z^Z��� 3N�;���5�I��C$���h����l���EЗӓ��Lw��U���:�QZ]A3��z��z`h���
U�`n,�V�/݊`�'Q5��L��8��I�&��J5�Pi*�� �_w��a�$��	��G���Y\w6�?�����M6�s��F��	��X��*4��U*H1H6�b����p��v�$4N ���D�=�>O3'k��$�K�J��^���e$��X>��O`n���x+� ���7>�s�[@8H�VJ�Ud2��2ʚ�$ƍ��`�砗�Q�'�"_cmT�\���&:�:���(f�\b�r��kq	:X��R�hM	Us}��8�EX����ԯ.&���C�t���d����#�}bT����<@�S1�@�`#�a;�0�����v,�O��p�/`1
�E��.�UtK��k`>#�ID�����#E�	��j�ߨ�~�~��YnbWb[�[�VC�A�݆-l�273��lc�`ϣ���.v}�~;�v���	/��籑��c�1�p�HB+�v�����+���c3G]��-[/�F��I�<���fJ���4iW(�����oPK�ʻd  �
  PK   km3J           	                META-INF/��  PK   km3JX�j�[   ^                =   META-INF/MANIFEST.MFPK   jm3J�ʻd  �
               �   uccm/GetUccm.classPK      �   ~    
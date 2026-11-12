#!/system/bin/sh
# Os arquivos em system/ são sobrepostos pelo Magisk na inicialização, sem gravar na partição.
ui_print "- Game Mode Service"
ui_print "- Reinicie o aparelho para ativar."
set_perm_recursive "$MODPATH/system/system_ext" 0 0 0755 0644

# Política de Privacidade – MyRingReminder

Last updated: 2026-11-05

## 1. Controlador
Daniel Sichler (Darexsh)  
E-mail: sichler.daniel@gmail.com

## 2. Escopo
Esta política se aplica ao aplicativo Android MyRingReminder (`com.darexsh.myringreminder`), distribuído por este repositório e por versões APK.

## 3. Dados tratados pelo MyRingReminder
O MyRingReminder trata dados do aplicativo necessários para acompanhar o ciclo e personalizar a experiência, incluindo:
- datas e histórico do ciclo
- registros menstruais (por exemplo, indicação de dia de menstruação, intensidade do sangramento, sintomas como dor ou doença e marcadores opcionais de início e fim)
- configurações de lembretes e horários de notificações
- anotações inseridas por você
- preferências de aparência (cores, plano de fundo e idioma)
- configurações opcionais de bloqueio do aplicativo

## 4. Armazenamento local de dados
O MyRingReminder armazena seus dados localmente no seu dispositivo (por exemplo, usando SharedPreferences ou o armazenamento local do aplicativo).

O MyRingReminder não envia seus dados pessoais do ciclo nem suas anotações ao desenvolvedor.

Se você usar o backup ou a restauração, os dados serão exportados ou importados como um arquivo local escolhido por você no seletor de arquivos do Android.

Se você exportar os detalhes menstruais em PDF, o arquivo será gerado localmente no seu dispositivo e salvo somente no local escolhido por você no seletor de arquivos do Android.  
Ao abrir esse PDF pela notificação do aplicativo, o Android poderá encaminhar o arquivo para o aplicativo de leitura de PDF escolhido por você. Nesse caso, esse aplicativo tratará o arquivo conforme a própria política de privacidade.

## 5. Permissões utilizadas
O MyRingReminder poderá solicitar as permissões necessárias para os recursos do aplicativo, como:
- notificações e alarmes exatos (lembretes do ciclo)
- acesso a arquivos ou mídia (seleção de imagem de plano de fundo, backup e restauração)
- dados biométricos ou credenciais do dispositivo (bloqueio opcional do aplicativo)

As permissões são usadas somente para fornecer os recursos do aplicativo solicitados.

## 6. Análise, rastreamento e relatórios de falhas
De acordo com a implementação atual, o MyRingReminder **não** inclui SDKs de terceiros para análise ou relatórios de falhas.

## 7. Rede e links externos
Os recursos principais do MyRingReminder funcionam localmente e não exigem uma API de backend dedicada para o acompanhamento do ciclo.

Para verificar atualizações, o MyRingReminder pode solicitar metadados de versões à API pública do GitHub Releases deste projeto.

Ao abrir **Configurações → Bula**, o aplicativo solicita o arquivo público `leaflet-packages.json` do repositório GitHub deste projeto. Ao abrir **Informações do app → Política de Privacidade**, ele solicita o arquivo da política correspondente ao idioma selecionado no aplicativo; se não houver tradução disponível, será usada a versão em inglês. Essas solicitações não incluem seus dados do ciclo, anotações ou configurações do aplicativo. O GitHub poderá tratar informações comuns de conexão, como seu endereço IP e os cabeçalhos da solicitação, de acordo com a política de privacidade dele. O aplicativo mantém no seu dispositivo a última cópia obtida com sucesso de cada idioma para que ela continue disponível caso uma solicitação posterior falhe.

Quando solicitado por você, o MyRingReminder pode abrir destinos externos por meio de intents do sistema, incluindo:
- e-mail (`mailto:`)
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- bulas hospedadas por serviços externos de informações sobre medicamentos ou fabricantes

Ao abrir uma bula, o Android encaminha o URL para o navegador ou visualizador de documentos escolhido por você. O serviço que hospeda o documento recebe a solicitação; o URL pode revelar o produto e o idioma da bula. O navegador, o visualizador e o serviço que hospeda a bula poderão tratar informações comuns de conexão conforme suas próprias políticas de privacidade. O MyRingReminder não inclui nessas solicitações os dados do ciclo, as anotações ou as configurações armazenadas localmente.

Ao abrir links externos, aplicam-se as políticas de privacidade dos respectivos serviços de terceiros.

### Serviços de terceiros mencionados nos links
- GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram: https://telegram.org/privacy
- Linktree: https://linktr.ee/s/privacy
- Buy Me a Coffee: https://www.buymeacoffee.com/privacy-policy

## 8. Compartilhamento de dados
De acordo com a implementação atual do aplicativo, o MyRingReminder não transmite ao desenvolvedor os dados do ciclo, as anotações ou as configurações armazenadas localmente.

## 9. Aviso médico
O MyRingReminder é apenas uma ferramenta de organização e acompanhamento.

Não é um dispositivo médico, não fornece diagnóstico médico e não substitui a orientação de um profissional de saúde.

Em caso de dúvidas médicas ou problemas de saúde, consulte um profissional de saúde qualificado.

## 10. Suas opções
- Você pode editar ou excluir seus dados no aplicativo.
- Você pode revogar as permissões do aplicativo nas configurações do Android.
- Você pode optar por não abrir links externos.
- Você pode desinstalar o aplicativo e apagar os dados locais nas configurações do Android.

## 11. Alterações nesta política
Esta política poderá ser atualizada se os recursos do aplicativo ou os requisitos legais mudarem.

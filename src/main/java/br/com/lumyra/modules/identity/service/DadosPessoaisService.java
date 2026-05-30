package br.com.lumyra.modules.identity.service;

import br.com.lumyra.modules.identity.dto.ExportacaoDadosResponse;

public interface DadosPessoaisService {

    /** Exporta os dados pessoais da aluna autenticada (portabilidade LGPD). */
    ExportacaoDadosResponse exportar();

    /** Eliminação LGPD: soft-delete da aluna autenticada (purge posterior). */
    void eliminar();
}

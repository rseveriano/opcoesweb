package br.eti.ranieri.opcoesweb.persistencia;

import java.util.Map;

import br.eti.ranieri.opcoesweb.estado.Serie;
import br.eti.ranieri.opcoesweb.estado.Variavel;

public class Opcao {
    private Serie serie;
    private String codigo;
    private Map<Variavel, Number> variaveis;
}

package br.eti.ranieri.opcoesweb.simulacao;

import java.io.Serializable;

import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.Acao;

public class ConfigSimulacao implements Serializable {
	public Acao acao;
	public LocalDate dataInicial;
	public LocalDate dataFinal;
	public Integer custodiaInicial;
	public Double prejuizoMaximo;
	public Integer tempoMaximoSimulacao;
	public Double corretagemIntegral;
	public Double corretagemOpcoes;
	public Double corretagemFracionario;
}

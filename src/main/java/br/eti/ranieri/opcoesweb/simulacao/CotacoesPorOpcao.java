package br.eti.ranieri.opcoesweb.simulacao;

import java.io.Serializable;
import java.util.List;

import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;

@Deprecated
public class CotacoesPorOpcao implements Serializable {

    private CotacaoOpcao opcao;
    private List<PrecoDataCodigo> precosDatasCodigos;

    public CotacoesPorOpcao(CotacaoOpcao opcao, List<PrecoDataCodigo> precosDatasCodigos) {
	this.opcao = opcao;
	this.precosDatasCodigos = precosDatasCodigos;
    }

    public CotacaoOpcao getOpcao() {
	return opcao;
    }

    public List<PrecoDataCodigo> getPrecosDatasCodigos() {
	return precosDatasCodigos;
    }

    @Deprecated
    public static class PrecoDataCodigo implements Serializable {
	private Double preco;
	private DataCodigo dataCodigo;

	public PrecoDataCodigo(Double preco, LocalDate data, String codigo) {
	    this.preco = preco;
	    this.dataCodigo = new DataCodigo(data, codigo);
	}

	public DataCodigo getDataCodigo() {
	    return dataCodigo;
	}

	public Double getPreco() {
	    return preco;
	}
    }

    @Deprecated
    public static class DataCodigo implements Serializable {
	private LocalDate data;
	private String codigo;

	public DataCodigo(LocalDate data, String codigo) {
	    this.data = data;
	    this.codigo = codigo;
	}

	public String getCodigo() {
	    return codigo;
	}

	public LocalDate getData() {
	    return data;
	}

	@Override
	public String toString() {
	    return "DataCodigo[codigo="+codigo+", data="+data+"]";
	}
    }
}

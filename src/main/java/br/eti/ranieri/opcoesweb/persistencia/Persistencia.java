package br.eti.ranieri.opcoesweb.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import br.eti.ranieri.opcoesweb.estado.Acao;
import br.eti.ranieri.opcoesweb.estado.CotacaoAcaoOpcoes;

public class Persistencia implements InitializingBean {

	private File arquivoPersistencia;
	private SortedMap<LocalDate, Map<Acao, CotacaoAcaoOpcoes>> cotacoesHistoricas;
	private SortedSet<Integer> cacheAnos;
	private Map<Integer, SortedSet<Integer>> cacheMeses;
	private Map<Integer, Map<Integer, SortedSet<Integer>>> cacheDias;

	public void setArquivoPersistencia(String nomeArquivoPersistencia) {
		this.arquivoPersistencia = new File(nomeArquivoPersistencia);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(arquivoPersistencia,
				"Propriedade arquivoPersistencia deve ser nao nula");
		if (arquivoPersistencia.exists()) {
			Assert.isTrue(arquivoPersistencia.canWrite(), String.format(
					"Deve ser possivel escrever no arquivo [%s]",
					arquivoPersistencia.getName()));

			try {
				ObjectInputStream input = new ObjectInputStream(
						new GZIPInputStream(new BufferedInputStream(
								new FileInputStream(arquivoPersistencia))));
				this.cotacoesHistoricas = (SortedMap<LocalDate, Map<Acao, CotacaoAcaoOpcoes>>) input
						.readObject();
				input.close();
			} catch (IOException e) {
				this.cotacoesHistoricas = new TreeMap<LocalDate, Map<Acao, CotacaoAcaoOpcoes>>();
				escreverCotacoesHistoricas();
			}
		} else {
			arquivoPersistencia.createNewFile();
			Assert.isTrue(arquivoPersistencia.canWrite(), String.format(
					"Deve ser possivel escrever no arquivo [%s]",
					arquivoPersistencia.getName()));

			this.cotacoesHistoricas = new TreeMap<LocalDate, Map<Acao, CotacaoAcaoOpcoes>>();
			escreverCotacoesHistoricas();
		}
		atualizarCaches();
	}

	public List<Integer> getAnos() {
		return new ArrayList<Integer>(cacheAnos);
		// Set<Integer> anos = new HashSet<Integer>();
		// for (LocalDate data : cotacoesHistoricas.keySet()) {
		// anos.add(data.getYear());
		// }
		// return new ArrayList<Integer>(new TreeSet<Integer>(anos));
	}

	public List<Integer> getMeses(Integer ano) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		SortedSet<Integer> meses = cacheMeses.get(ano);
		if (meses == null)
			return new ArrayList<Integer>();
		return new ArrayList<Integer>(meses);
		// Set<Integer> meses = new HashSet<Integer>();
		// for (LocalDate data : cotacoesHistoricas.keySet()) {
		// if (data.getYear() == ano) {
		// meses.add(data.getMonthOfYear());
		// }
		// }
		// return new ArrayList<Integer>(new TreeSet<Integer>(meses));
	}

	public List<Integer> getDias(Integer ano, Integer mes) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");
		Map<Integer, SortedSet<Integer>> diasPorMes = cacheDias.get(ano);
		if (diasPorMes == null)
			return new ArrayList<Integer>();
		SortedSet<Integer> dias = diasPorMes.get(mes);
		if (dias == null)
			return new ArrayList<Integer>();
		return new ArrayList<Integer>(dias);
		// Set<Integer> dias = new HashSet<Integer>();
		// for (LocalDate data : cotacoesHistoricas.keySet()) {
		// if (data.getYear() == ano && data.getMonthOfYear() == mes) {
		// dias.add(data.getDayOfMonth());
		// }
		// }
		// return new ArrayList<Integer>(new TreeSet<Integer>(dias));
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterPorData(Integer ano, Integer mes,
			Integer dia) {

		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");
		Assert.notNull(dia, "Dia deve ser nao-nulo");
		LocalDate data = criarDataFimPregao(ano, mes, dia);
		return cotacoesHistoricas.get(data);
	}

	public Map<Acao, CotacaoAcaoOpcoes> obterUltima() {
		return cotacoesHistoricas.get(cotacoesHistoricas.lastKey());
	}

	public List<Entry<LocalDate,CotacaoAcaoOpcoes>> getCotacoes(Acao acao, LocalDate dataInicial,
			LocalDate dataFinal) {

		List<Entry<LocalDate,CotacaoAcaoOpcoes>> cotacoes = new ArrayList<Entry<LocalDate,CotacaoAcaoOpcoes>>();
		for (LocalDate data : cotacoesHistoricas.keySet()) {
			if (data.isBefore(dataInicial) == false && data.isAfter(dataFinal) == false) {
				cotacoes.add(new SimpleEntry<LocalDate, CotacaoAcaoOpcoes>(data,
						cotacoesHistoricas.get(data).get(acao)));
			}
		}
		return cotacoes;
	}

	public synchronized void incluirCotacaoHistorica(LocalDate data, Acao acao,
			CotacaoAcaoOpcoes cotacoes) {

		Assert.notNull(data);
		Assert.notNull(acao);
		Assert.notNull(cotacoes);

		Map<Acao, CotacaoAcaoOpcoes> mapa = cotacoesHistoricas.get(data);
		if (mapa == null) {
			mapa = new HashMap<Acao, CotacaoAcaoOpcoes>();
			cotacoesHistoricas.put(data, mapa);
		}
		mapa.put(acao, cotacoes);
		atualizarCaches(data);
	}

	public synchronized void escreverCotacoesHistoricas() throws IOException {
		ObjectOutputStream output = new ObjectOutputStream(
				new GZIPOutputStream(new BufferedOutputStream(
						new FileOutputStream(arquivoPersistencia))));
		output.writeObject(this.cotacoesHistoricas);
		output.close();
	}

	protected LocalDate criarDataFimPregao(Integer ano, Integer mes,
			Integer dia) {
		Assert.notNull(ano, "Ano deve ser nao-nulo");
		Assert.notNull(mes, "Mes deve ser nao-nulo");
		Assert.notNull(dia, "Dia deve ser nao-nulo");
		return new LocalDate(ano, mes, dia);
	}

	protected synchronized void atualizarCaches(LocalDate novaData) {
		Assert.notNull(novaData);
		if (cacheAnos == null)
			cacheAnos = new TreeSet<Integer>();
		if (cacheMeses == null)
			cacheMeses = new HashMap<Integer, SortedSet<Integer>>();
		if (cacheDias == null)
			cacheDias = new HashMap<Integer, Map<Integer, SortedSet<Integer>>>();

		int ano = novaData.getYear();
		int mes = novaData.getMonthOfYear();
		int dia = novaData.getDayOfMonth();

		cacheAnos.add(ano);

		if (cacheMeses.get(ano) == null)
			cacheMeses.put(ano, new TreeSet<Integer>());
		cacheMeses.get(ano).add(mes);

		if (cacheDias.get(ano) == null)
			cacheDias.put(ano, new HashMap<Integer, SortedSet<Integer>>());
		if (cacheDias.get(ano).get(mes) == null)
			cacheDias.get(ano).put(mes, new TreeSet<Integer>());
		cacheDias.get(ano).get(mes).add(dia);
	}
	
	protected synchronized void atualizarCaches() {
		Assert.notNull(cotacoesHistoricas);

		if (cacheAnos == null)
			cacheAnos = new TreeSet<Integer>();
		if (cacheMeses == null)
			cacheMeses = new HashMap<Integer, SortedSet<Integer>>();
		if (cacheDias == null)
			cacheDias = new HashMap<Integer, Map<Integer, SortedSet<Integer>>>();

		for (LocalDate data : cotacoesHistoricas.keySet()) {
			atualizarCaches(data);
		}
	}
}

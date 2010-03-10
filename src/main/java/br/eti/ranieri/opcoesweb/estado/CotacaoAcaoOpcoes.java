/*
 *  Copyright 2009 ranieri.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package br.eti.ranieri.opcoesweb.estado;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author ranieri
 */
public class CotacaoAcaoOpcoes implements Serializable {

	private CotacaoAcao cotacaoAcao;
	private List<CotacaoOpcao> opcoesSerie1;
	private List<CotacaoOpcao> opcoesSerie2;
//	private CotacaoOpcoes serie1;
//	private CotacaoOpcoes serie2;

//	public CotacaoAcaoOpcoes(CotacaoAcao acao, CotacaoOpcoes serie1,
//			CotacaoOpcoes serie2) {
	public CotacaoAcaoOpcoes(CotacaoAcao acao, List<CotacaoOpcao> opcoesSerie1,
			List<CotacaoOpcao> opcoesSerie2) {

		this.cotacaoAcao = acao;
		this.opcoesSerie1 = opcoesSerie1;
		this.opcoesSerie2 = opcoesSerie2;
//		this.serie1 = serie1;
//		this.serie2 = serie2;
	}

	public CotacaoAcao getCotacaoAcao() {
		return cotacaoAcao;
	}

	public List<CotacaoOpcao> getOpcoesSerie1() {
		return opcoesSerie1;
	}

	public List<CotacaoOpcao> getOpcoesSerie2() {
		return opcoesSerie2;
	}

//	public CotacaoOpcoes getSerie1() {
//		return serie1;
//	}
//
//	public CotacaoOpcoes getSerie2() {
//		return serie2;
//	}

}

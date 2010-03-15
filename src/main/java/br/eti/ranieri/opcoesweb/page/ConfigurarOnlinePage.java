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
package br.eti.ranieri.opcoesweb.page;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.UrlValidator;

import br.eti.ranieri.opcoesweb.OpcoesWebHttpSession;
import br.eti.ranieri.opcoesweb.estado.ConfiguracaoOnline;

/**
 *
 * @author ranieri
 */
public class ConfigurarOnlinePage extends PaginaBase {

    public ConfigurarOnlinePage() {
        add(new FeedbackPanel("feedback"));
        
        final ConfiguracaoOnline configuracao = OpcoesWebHttpSession.get().getConfiguracaoOnline();
        Form formulario = new Form("formulario", new CompoundPropertyModel(configuracao)) {

            @Override
            protected void onSubmit() {
                setResponsePage(ExibirOnlinePage.class);
            }
        };
        add(formulario);

        formulario.add(new CheckBox("usarProxy"));

        final TextField proxyURL = new TextField("proxyURL");
        proxyURL.add(new INullAcceptingValidator() {
            public void validate(IValidatable validatable) {
                if (configuracao.isUsarProxy() && validatable.getValue() == null) {
                    ValidationError error = new ValidationError();
                    error.setMessage("Se quiser utilizar proxy, deve definir sua URL, no formato [http://proxy.dominio.com:3128]");
                    validatable.error(error);
                }
            }
        });
        proxyURL.add(new UrlValidator(new String[]{"http"}, UrlValidator.NO_FRAGMENTS));
        formulario.add(proxyURL);
        formulario.add(new RequiredTextField("jsessionid"));
        formulario.add(new Button("submeter"));
    }
}

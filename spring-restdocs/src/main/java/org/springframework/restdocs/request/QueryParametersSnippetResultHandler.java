/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.restdocs.request;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.restdocs.snippet.DocumentationWriter;
import org.springframework.restdocs.snippet.DocumentationWriter.TableAction;
import org.springframework.restdocs.snippet.DocumentationWriter.TableWriter;
import org.springframework.restdocs.snippet.SnippetGenerationException;
import org.springframework.restdocs.snippet.SnippetWritingResultHandler;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;

/**
 * A {@link SnippetWritingResultHandler} that produces a snippet documenting the query
 * parameters supported by a RESTful resource.
 * 
 * @author Andy Wilkinson
 */
public class QueryParametersSnippetResultHandler extends SnippetWritingResultHandler {

	private final Map<String, ParameterDescriptor> descriptorsByName = new LinkedHashMap<>();

	protected QueryParametersSnippetResultHandler(String outputDir,
			ParameterDescriptor... descriptors) {
		super(outputDir, "query-parameters");
		for (ParameterDescriptor descriptor : descriptors) {
			Assert.hasText(descriptor.getName());
			Assert.hasText(descriptor.getDescription());
			this.descriptorsByName.put(descriptor.getName(), descriptor);
		}
	}

	@Override
	protected void handle(MvcResult result, DocumentationWriter writer)
			throws IOException {
		verifyParameterDescriptors(result);
		documentParameters(writer);
	}

	private void verifyParameterDescriptors(MvcResult result) {
		Set<String> actualParameters = result.getRequest().getParameterMap().keySet();
		Set<String> expectedParameters = this.descriptorsByName.keySet();

		Set<String> undocumentedParameters = new HashSet<String>(actualParameters);
		undocumentedParameters.removeAll(expectedParameters);

		Set<String> missingParameters = new HashSet<String>(expectedParameters);
		missingParameters.removeAll(actualParameters);

		if (!undocumentedParameters.isEmpty() || !missingParameters.isEmpty()) {
			String message = "";
			if (!undocumentedParameters.isEmpty()) {
				message += "Query parameters with the following names were not documented: "
						+ undocumentedParameters;
			}
			if (!missingParameters.isEmpty()) {
				if (message.length() > 0) {
					message += ". ";
				}
				message += "Query parameters with the following names were not found in the request: "
						+ missingParameters;
			}
			throw new SnippetGenerationException(message);
		}

		Assert.isTrue(actualParameters.equals(expectedParameters));
	}

	private void documentParameters(DocumentationWriter writer) throws IOException {
		writer.table(new TableAction() {

			@Override
			public void perform(TableWriter tableWriter) throws IOException {
				tableWriter.headers("Parameter", "Description");
				for (Entry<String, ParameterDescriptor> entry : QueryParametersSnippetResultHandler.this.descriptorsByName
						.entrySet()) {
					tableWriter.row(entry.getKey(), entry.getValue().getDescription());
				}
			}

		});
	}

}

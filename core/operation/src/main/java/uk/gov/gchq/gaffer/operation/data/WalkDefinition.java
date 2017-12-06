/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.operation.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.gov.gchq.gaffer.data.element.id.ElementId;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;
import uk.gov.gchq.gaffer.operation.io.Input;
import uk.gov.gchq.gaffer.operation.io.Output;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * A {@code WalkDefinition} describes how to carry out a single step in a {@link uk.gov.gchq.gaffer.data.graph.Walk}.
 */
@JsonPropertyOrder({"preFilters", "operation", "postFilters"})
public class WalkDefinition implements Cloneable {

    private final OperationChain<Iterable<ElementId>> preFilters;
    private final OperationChain<Iterable<ElementId>> postFilters;
    private final GetElements operation;

    public WalkDefinition(final Builder builder) {
        this.preFilters = builder.preFilters;
        this.postFilters = builder.postFilters;
        this.operation = builder.operation;
    }

    /**
     * Public constructor used by Jackson.
     *
     * @param preFilters  the preFilter operation chain
     * @param postFilters the postFilter operation chain
     * @param operation   the GetElements operation
     */
    @JsonCreator
    public WalkDefinition(@JsonProperty("preFilters") final OperationChain<Iterable<ElementId>> preFilters,
                          @JsonProperty("postFilters") final OperationChain<Iterable<ElementId>> postFilters,
                          @JsonProperty("operation") final GetElements operation) {
        this.preFilters = (null != preFilters) ? preFilters : new OperationChain<>();
        this.postFilters = (null != postFilters) ? postFilters : new OperationChain<>();
        this.operation = operation;
    }

    public OperationChain<Iterable<ElementId>> getPostFilters() {
        return postFilters;
    }

    public GetElements getOperation() {
        return operation;
    }

    public OperationChain<Iterable<ElementId>> getPreFilters() {
        return preFilters;
    }

    @JsonIgnore
    public List<Operation> getPreFiltersList() {
        return preFilters.getOperations();
    }

    @JsonIgnore
    public List<Operation> getPostFiltersList() {
        return postFilters.getOperations();
    }

    @JsonIgnore
    public List<Operation> asList() {
        return Stream.of(getPostFiltersList(), Collections.singletonList(operation), getPostFiltersList())
                .flatMap(List::stream)
                .collect(toList());
    }

    @JsonIgnore
    public OperationChain<Iterable<ElementId>> asChain() {
        return new OperationChain<>(asList());
    }

    @Override
    public WalkDefinition clone() {
        final WalkDefinition.Builder builder = new WalkDefinition.Builder();

        builder.postFilters = postFilters.shallowClone();
        builder.preFilters = preFilters.shallowClone();
        builder.operation = operation.shallowClone();

        return builder.build();
    }

    public static class Builder {
        private OperationChain< Iterable<ElementId>> preFilters;
        private OperationChain< Iterable<ElementId>> postFilters;

        private OperationChain.OutputBuilder<Iterable<ElementId>> preFiltersBuilder;
        private OperationChain.OutputBuilder<Iterable<ElementId>> postFiltersBuilder;

        private GetElements operation;

        public Builder preFilter(final Operation operation) {
            if (null == preFiltersBuilder) {
                preFiltersBuilder = new OperationChain.Builder().first((Output) operation);
            } else {
                preFiltersBuilder.then((Input) operation);
            }
            return this;
        }

        public Builder postFilter(final Operation operation) {
            if (null == postFiltersBuilder) {
                postFiltersBuilder = new OperationChain.Builder().first((Output) operation);
            } else {
                postFiltersBuilder.then((Input) operation);
            }
            return this;
        }

        public Builder operation(final GetElements operation) {
            this.operation = operation;
            return this;
        }

        public WalkDefinition build() {
            if (null == preFilters) {
                if (null != preFiltersBuilder) {
                    preFilters = preFiltersBuilder.build();
                } else {
                    preFilters = new OperationChain<>();
                }
            }

            if (null == postFilters) {
                if (null != postFiltersBuilder) {
                    postFilters = postFiltersBuilder.build();
                } else {
                    postFilters = new OperationChain<>();
                }
            }
            return new WalkDefinition(this);
        }
    }

}

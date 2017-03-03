/*
 * Copyright 2016 Crown Copyright
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

package uk.gov.gchq.gaffer.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.gaffer.data.Validator;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.elementdefinition.view.ViewElementDefinition;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaElementDefinition;

/**
 * An <code>ElementValidator</code> is a {@link Validator} for {@link Element}s
 * It is capable of validating an {@link Element} based on {@link uk.gov.gchq.gaffer.function.FilterFunction}s
 * in {@link Schema} or {@link View}.
 */
public class ElementValidator implements Validator<Element> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementValidator.class);
    private final Schema schema;
    private final View view;
    private final boolean includeIsA;

    public enum FilterType {
        PRE_AGGREGATION_FILTER, POST_AGGREGATION_FILTER, POST_TRANSFORM_FILTER
    }

    /**
     * Constructs a <code>ElementValidator</code> with a {@link Schema} to use to
     * validate {@link Element}s.
     *
     * @param schema the {@link Schema} to use to
     *               validate {@link Element}s.
     */
    public ElementValidator(final Schema schema) {
        this(schema, true);
    }

    /**
     * Constructs a <code>ElementValidator</code> with a {@link Schema} to use to
     * validate {@link uk.gov.gchq.gaffer.data.element.Element}s. Uses the includeIsA flag
     * to determine whether the IsA validate functions should be used. Disabling
     * them can be useful when you already know the data is of the correct type
     * and therefore you are able to improve the performance.
     *
     * @param schema     the {@link Schema} to use to
     *                   validate {@link uk.gov.gchq.gaffer.data.element.Element}s.
     * @param includeIsA if true then the ISA validate functions are used, otherwise they are skipped.
     */
    public ElementValidator(final Schema schema, final boolean includeIsA) {
        this.schema = schema;
        this.view = null;
        this.includeIsA = includeIsA;
    }

    /**
     * Constructs a <code>ElementValidator</code> with a {@link View} to use to
     * validate {@link Element}s.
     *
     * @param view the {@link View} to use to
     *             validate {@link Element}s.
     */
    public ElementValidator(final View view) {
        this.view = view;
        this.schema = null;
        includeIsA = false;
    }

    /**
     * @param element the {@link Element} to validate
     * @return true if the provided {@link Element} is valid,
     * otherwise false and the reason will be logged.
     */
    @Override
    public boolean validate(final Element element) {
        if (null == element) {
            return false;
        }

        if (null != schema) {
            return validateWithSchema(element);
        }

        if (!validateAgainstViewFilter(element, FilterType.PRE_AGGREGATION_FILTER)) {
            return false;
        }
        if (!validateAgainstViewFilter(element, FilterType.POST_AGGREGATION_FILTER)) {
            return false;
        }
        return validateAgainstViewFilter(element, FilterType.POST_TRANSFORM_FILTER);

    }

    public boolean validateInput(final Element element) {
        return validateAgainstViewFilter(element, FilterType.PRE_AGGREGATION_FILTER);
    }

    public boolean validateAggregation(final Element element) {
        return validateAgainstViewFilter(element, FilterType.POST_AGGREGATION_FILTER);
    }

    public boolean validateTransform(final Element element) {
        return validateAgainstViewFilter(element, FilterType.POST_TRANSFORM_FILTER);
    }

    public boolean validateAggregationTransformation(final Element element) {
        return validateAgainstViewFilter(element, FilterType.POST_AGGREGATION_FILTER) && validateAgainstViewFilter(element, FilterType.POST_TRANSFORM_FILTER);
    }

    public boolean validateWithSchema(final Element element) {
        final SchemaElementDefinition elementDef = schema.getElement(element.getGroup());
        if (null == elementDef) {
            LOGGER.warn("No element definition found for : " + element.getGroup());
            return false;
        }

        return elementDef.getValidator(includeIsA).filter(element);
    }

    private boolean validateAgainstViewFilter(final Element element, final FilterType filterType) {
        if (null == element) {
            return false;
        }
        if (null != schema) {
            return validateWithSchema(element);
        }
        final ViewElementDefinition elementDef = view.getElement(element.getGroup());
        if (null == elementDef) {
            return false;
        }

        final ElementFilter validator = getElementFilter(elementDef, filterType);
        return null == validator || validator.filter(element);
    }

    private ElementFilter getElementFilter(final ViewElementDefinition elementDef, final FilterType filterType) {
        if (filterType == FilterType.PRE_AGGREGATION_FILTER) {
            return elementDef.getPreAggregationFilter();
        } else if (filterType == FilterType.POST_AGGREGATION_FILTER) {
            return elementDef.getPostAggregationFilter();
        } else {
            return elementDef.getPostTransformFilter();
        }
    }

}

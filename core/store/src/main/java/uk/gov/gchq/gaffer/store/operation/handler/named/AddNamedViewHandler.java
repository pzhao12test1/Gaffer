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

package uk.gov.gchq.gaffer.store.operation.handler.named;

import uk.gov.gchq.gaffer.data.elementdefinition.view.NamedView;
import uk.gov.gchq.gaffer.named.operation.cache.exception.CacheOperationFailedException;
import uk.gov.gchq.gaffer.named.view.AddNamedView;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;
import uk.gov.gchq.gaffer.store.operation.handler.named.cache.NamedViewCache;

public class AddNamedViewHandler implements OperationHandler<AddNamedView> {
    private final NamedViewCache cache;


    public AddNamedViewHandler() {
        this(new NamedViewCache());
    }

    public AddNamedViewHandler(final NamedViewCache cache) {
        this.cache = cache;
    }

    @Override
    public Object doOperation(final AddNamedView operation, final Context context, final Store store) throws OperationException {
        validate(operation);

        final NamedView namedView = new NamedView.Builder()
                .name(operation.getNamedView().getName())
                .merge(operation.getNamedView())
                .parameters(operation.getParameters())
                .build();

        try {
            cache.addNamedView(namedView, operation.isOverwriteFlag());
        } catch (final CacheOperationFailedException e) {
            throw new OperationException(e.getMessage(), e);
        }
        return null;
    }

    private void validate(final AddNamedView op) {
        if (null == op.getNamedView().getName()) {
            throw new IllegalArgumentException("NamedView name must be set");
        }
    }
}
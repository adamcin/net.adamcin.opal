/*
 * Copyright 2017 Mark Adamcin
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

package net.adamcin.oakpal.core.jcrfacade.query;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import net.adamcin.oakpal.core.jcrfacade.RangeIteratorFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;

/**
 * Wraps {@link RowIterator} to ensure returned objects are wrapped appropriately.
 */
public class RowIteratorFacade extends RangeIteratorFacade<RowIterator> implements RowIterator {
    private final SessionFacade session;

    public RowIteratorFacade(RowIterator delegate, SessionFacade session) {
        super(delegate);
        this.session = session;
    }

    @Override
    public Row nextRow() {
        Row internal = delegate.nextRow();
        return new RowFacade(internal, session);
    }

    @Override
    public Object next() {
        return nextRow();
    }
}

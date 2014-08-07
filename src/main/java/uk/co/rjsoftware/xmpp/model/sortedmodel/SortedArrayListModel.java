/*
 * Copyright (c) 2014, Richard Simpson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.rjsoftware.xmpp.model.sortedmodel;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortedArrayListModel<E> extends AbstractListModel<E> {

    private final ListModel<E> unsortedModel;
    private final SortOrder sortOrder;
    private final Comparator comparator;
    private final ArrayList<SortedListEntry> sortedModel;

    public SortedArrayListModel(final ListModel<E> unsortedModel, final SortOrder sortOrder, final Comparator comp) {
        this.unsortedModel = unsortedModel;
        this.unsortedModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent event) {
                unsortedIntervalAdded(event);
            }

            @Override
            public void intervalRemoved(ListDataEvent event) {
                unsortedIntervalRemoved(event);
            }

            @Override
            public void contentsChanged(ListDataEvent event) {
                unsortedContentsChanged(event);
            }
        });

        this.sortOrder = sortOrder;

        if (null == comp) {
            this.comparator = Collator.getInstance();
        }
        else {
            this.comparator = comp;
        }

        // get base model info
        final int size = unsortedModel.getSize();
        this.sortedModel = new ArrayList<SortedListEntry>(size);
        for (int x = 0 ; x<size ; ++x) {
            SortedListEntry entry = new SortedListEntry(x);
            int insertionPoint = findInsertionPoint(entry);
            this.sortedModel.add(insertionPoint, entry);
        }
    }

    private int findInsertionPoint(final SortedListEntry entry) {
        int insertionPoint = sortedModel.size();
        if (sortOrder != SortOrder.UNSORTED)  {
            insertionPoint = Collections.binarySearch((List) sortedModel, entry);
            if (insertionPoint < 0) {
                insertionPoint = -(insertionPoint +1);
            }
        }
        return insertionPoint;
    }

    private void unsortedContentsChanged(ListDataEvent e) {
        Collections.sort(sortedModel);
        fireContentsChanged(this, 0, sortedModel.size()-1);
    }

    private void unsortedIntervalAdded(ListDataEvent e) {
        int begin = e.getIndex0();
        int end = e.getIndex1();
        int nElementsAdded = end-begin+1;

        /* Items in the decorated model have shifted position.
         * Increment model pointers into the decorated model.
         * Increment indices that intersect with the insertion
         * point in the decorated model.
         */
        for (SortedListEntry entry: sortedModel) {
            int index = entry.getIndex();
            // If the model points to a model index >= to where
            // new model entries are added, increment their index.
            if (index >= begin) {
                entry.setIndex(index+nElementsAdded);
            }
        }

        // Now add the new items from the decorated model
        // and notify listeners.
        for (int x = begin; x <= end; ++x) {
            SortedListEntry newentry = new SortedListEntry(x);
            int insertionpoint = findInsertionPoint(newentry);
            this.sortedModel.add(insertionpoint, newentry);
            fireIntervalAdded(this, insertionpoint,
                    insertionpoint);
        }
    }

    /**
     * Update this model when items are removed from the
     * original/decorated model. Also, let listeners know that
     * items have been removed.
     */
    private void unsortedIntervalRemoved(ListDataEvent e) {
        int begin = e.getIndex0();
        int end = e.getIndex1();
        int nElementsRemoved = end-begin+1;

        /*
         * Move from end to beginning of our sorted model, updating
         * element indices into the decorated model or removing
         * elements as necessary.
         */
        int sortedSize = sortedModel.size();
        boolean[] bElementRemoved = new boolean[sortedSize];
        for (int x = sortedSize-1; x >=0; --x) {
            SortedListEntry entry = sortedModel.get(x);
            int index = entry.getIndex();
            if (index > end) {
                entry.setIndex(index - nElementsRemoved);
            } else if (index >= begin) {
                sortedModel.remove(x);
                bElementRemoved[x] = true;
            }
        }
        // Let listeners know about removed items.
        for(int x = bElementRemoved.length-1; x>=0; --x) {
            if (bElementRemoved[x]) {
                fireIntervalRemoved(this, x, x);
            }
        }
    }

    /**
     * Convert sorted-model index to an unsorted-model index.
     * @param index an index in the sorted model
     * @return modelIndex an index in the unsorted model
     */
    public int toUnsortedModelIndex(int index) {
        int modelIndex = -1;
        final SortedListEntry entry = this.sortedModel.get(index);
        modelIndex = entry.getIndex();
        return modelIndex;
    }

    @Override
    public E getElementAt(int index) {
        int modelIndex = toUnsortedModelIndex(index);
        E element = (E)unsortedModel.getElementAt(modelIndex);
        return element;
    }

    @Override
    public int getSize() {
        return sortedModel.size();
    }

    private class SortedListEntry implements Comparable {

        private int index;

        public SortedListEntry(final int index) {
            this.index = index;
        }

        public int compareTo(Object o) {
            // Retrieve the element that this entry points to
            // in the original model.
            Object thisElement = unsortedModel.getElementAt(index);
            SortedListEntry thatEntry = (SortedListEntry)o;
            // Retrieve the element that thatEntry points to
            // in the original model.
            Object thatElement = unsortedModel.getElementAt(thatEntry.index);
            if (comparator instanceof Collator) {
                thisElement = thisElement.toString();
                thatElement = thatElement.toString();
            }
            // Compare the base model's elements using the provided comparator.
            int comparison = comparator.compare(thisElement, thatElement);
            // Convert to descending order as necessary.
            if (sortOrder == SortOrder.DESCENDING) {
                comparison = -comparison;
            }
            return comparison;
        }

        public int getIndex() {
            return this.index;
        }

        public void setIndex(final int index) {
            this.index = index;
        }
    }

}

package org.eclipse.swt.widgets;

/*
 * Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.internal.carbon.DataBrowserListViewColumnDesc;
import org.eclipse.swt.internal.carbon.DataBrowserCallbacks;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;

public class Tree extends Composite {
	TreeItem [] items;
	boolean ignoreSelect;
	int anchorFirst, anchorLast;
	static final int COLUMN_ID = 1024;
	static final int CHECK_COLUMN_ID = 1025;

public Tree (Composite parent, int style) {
	super (parent, checkStyle (style));
}

public void addSelectionListener(SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Selection, typedListener);
	addListener (SWT.DefaultSelection, typedListener);
}

public void addTreeListener(TreeListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Expand, typedListener);
	addListener (SWT.Collapse, typedListener);
} 

static int checkStyle (int style) {
	/*
	* Feature in Windows.  It is not possible to create
	* a tree that scrolls and does not have scroll bars.
	* The TVS_NOSCROLL style will remove the scroll bars
	* but the tree will never scroll.  Therefore, no matter
	* what style bits are specified, set the H_SCROLL and
	* V_SCROLL bits so that the SWT style will match the
	* widget that Windows creates.
	*/
	style |= SWT.H_SCROLL | SWT.V_SCROLL;
	return checkBits (style, SWT.SINGLE, SWT.MULTI, 0, 0, 0, 0);
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget ();
	//NOT DONE
	return new Point (200, 200);
}

void createHandle () {
	Display display = getDisplay ();
	int [] outControl = new int [1];
	int window = OS.GetControlOwner (parent.handle);
	OS.CreateDataBrowserControl (window, null, OS.kDataBrowserListView, outControl);
	if (outControl [0] == 0) error (SWT.ERROR_NO_HANDLES);
	handle = outControl [0];
	int selectionFlags = OS.kDataBrowserSelectOnlyOne;
	if ((style & SWT.MULTI) != 0) selectionFlags = OS.kDataBrowserCmdTogglesSelection;
	OS.SetDataBrowserSelectionFlags (handle, selectionFlags);
	OS.SetDataBrowserListViewHeaderBtnHeight (handle, (short) 0);
	OS.SetDataBrowserHasScrollBars (handle, (style & SWT.H_SCROLL) != 0, (style & SWT.V_SCROLL) != 0);
	//NOT DONE
	if ((style & SWT.H_SCROLL) == 0) OS.AutoSizeDataBrowserListViewColumns (handle);
	if ((style & SWT.CHECK) != 0) {
		DataBrowserListViewColumnDesc checkColumn= new DataBrowserListViewColumnDesc ();
		checkColumn.headerBtnDesc_version = OS.kDataBrowserListViewLatestHeaderDesc;
		checkColumn.propertyDesc_propertyID = CHECK_COLUMN_ID;
		checkColumn.propertyDesc_propertyType = OS.kDataBrowserCheckboxType;
		checkColumn.propertyDesc_propertyFlags = OS.kDataBrowserPropertyIsMutable;
		//NOT DONE
		checkColumn.headerBtnDesc_minimumWidth = 40;
		checkColumn.headerBtnDesc_maximumWidth = 40;
		checkColumn.headerBtnDesc_initialOrder = OS.kDataBrowserOrderIncreasing;
		OS.AddDataBrowserListViewColumn (handle, checkColumn, 0);
	}
	DataBrowserListViewColumnDesc column = new DataBrowserListViewColumnDesc ();
	column.headerBtnDesc_version = OS.kDataBrowserListViewLatestHeaderDesc;
	column.propertyDesc_propertyID = COLUMN_ID;
	column.propertyDesc_propertyType = OS.kDataBrowserTextType; // OS.kDataBrowserIconAndTextType
	column.propertyDesc_propertyFlags = OS.kDataBrowserListViewSelectionColumn | OS.kDataBrowserDefaultPropertyFlags;
	//NOT DONE
	column.headerBtnDesc_maximumWidth= 300;
	column.headerBtnDesc_initialOrder= OS.kDataBrowserOrderIncreasing;
	OS.AddDataBrowserListViewColumn (handle, column, 1);
	OS.SetDataBrowserListViewDisclosureColumn (handle, COLUMN_ID, true);
	OS.HIViewAddSubview (parent.handle, handle);
	OS.HIViewSetZOrder (handle, OS.kHIViewZOrderBelow, 0);
}

void createItem (TreeItem item, TreeItem parentItem, int index) {
	int count = 0;
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == parentItem) count++;
	}
	if (index == -1) index = count;
	item.index = index;
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == parentItem) {
			if (items [i].index >= item.index) items [i].index++;
		}
	}
	int id = 0;
	while (id < items.length && items [id] != null) id++;
	if (id == items.length) {
		TreeItem [] newItems = new TreeItem [items.length + 4];
		System.arraycopy (items, 0, newItems, 0, items.length);
		items = newItems;
	}
	items [id] = item;
	item.id = id + 1;
	int parentID = OS.kDataBrowserNoItem;
	boolean expanded = true;
	if (parentItem != null) {
		parentID = parentItem.id;
		expanded = parentItem.getExpanded ();
	}
	if (expanded) {
		if (OS.AddDataBrowserItems (handle, parentID, 1, new int[] {item.id}, 0) != OS.noErr) {
			items [id] = null;
			error (SWT.ERROR_ITEM_NOT_ADDED);
		}
	}
}

void createWidget () {
	super.createWidget ();
	items = new TreeItem [4];
}

public void deselectAll () {
	checkWidget ();
	ignoreSelect = true;
	OS.SetDataBrowserSelectedItems (handle, 0, null, OS.kDataBrowserItemsRemove);
	ignoreSelect = false;
}

void destroyItem (TreeItem item) {
	int parentID = item.parentItem == null ? OS.kDataBrowserNoItem : item.parentItem.id;
	if (OS.RemoveDataBrowserItems (handle, parentID, 1, new int[] {item.id}, 0) != OS.noErr) {
		error (SWT.ERROR_ITEM_NOT_REMOVED);
	}
	for (int i=0; i<items.length; i++) {
		if (items [i] != null) {
			TreeItem parentItem = items [i].parentItem;
			while (parentItem != null && parentItem != item) {
				parentItem = parentItem.parentItem;
			}
			if (parentItem == item) {
				TreeItem oldItem = items [i];
				items [i].id = 0;
				items [i].index = -1;	
				items [i] = null;
				oldItem.releaseResources ();
			}
		}
	}
	TreeItem parentItem = item.parentItem;
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == parentItem) {
			if (items [i].index >= item.index) --items [i].index;
		}
	}
	items [item.id - 1] = null;
	item.id = 0;
	item.index = -1;
}

public TreeItem getItem (Point point) {
	checkWidget ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	return null;
}

public int getItemCount () {
	checkWidget ();
	return getItemCount (null);
}

int getItemCount (TreeItem item) {
	checkWidget ();
	int count = 0;
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == item) count++;
	}
	return count;
}

public int getItemHeight () {
	checkWidget ();
	short [] height = new short [1];
	if (OS.GetDataBrowserTableViewRowHeight (handle, height) != OS.noErr) {
		error (SWT.ERROR_CANNOT_GET_ITEM_HEIGHT);
	}
	return height [0];
}

public TreeItem [] getItems () {
	checkWidget ();
	return getItems (null);
}

public TreeItem [] getItems (TreeItem item) {
	int count = 0;
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == item) count++;
	}
	TreeItem [] result = new TreeItem [count];
	for (int i=0; i<items.length; i++) {
		if (items [i] != null && items [i].parentItem == item) {
			result [items [i].index] = items [i];
		}
	}
	return result;
}

public TreeItem getParentItem () {
	checkWidget ();
	return null;
}

public TreeItem [] getSelection () {
	checkWidget ();
	int ptr = OS.NewHandle (0);
	if (OS.GetDataBrowserItems (handle, OS.kDataBrowserNoItem, true, OS.kDataBrowserItemIsSelected, ptr) != OS.noErr) {
		error (SWT.ERROR_CANNOT_GET_SELECTION);
	}
	int count = OS.GetHandleSize (ptr) / 4;
	TreeItem [] result = new TreeItem [count];
	OS.HLock (ptr);
	int [] start = new int [1];
	OS.memcpy (start, ptr, 4);
	int [] id = new int [1];
	for (int i=0; i<count; i++) {
		OS.memcpy (id, start [0] + (i * 4), 4);
		result [i] = items [id [0] - 1];
	}
	OS.HUnlock (ptr);
	OS.DisposeHandle (ptr);
	return result;
}

public int getSelectionCount () {
	checkWidget ();
	int [] count = new int [1];
	if (OS.GetDataBrowserItemCount (handle, OS.kDataBrowserNoItem, true, OS.kDataBrowserItemIsSelected, count) != OS.noErr) {
		error (SWT.ERROR_CANNOT_GET_COUNT);
	}
	return count [0];
}

void hookEvents () {
	super.hookEvents ();
	Display display= getDisplay();
	DataBrowserCallbacks callbacks = new DataBrowserCallbacks ();
	callbacks.version = OS.kDataBrowserLatestCallbacks;
	OS.InitDataBrowserCallbacks (callbacks);
	callbacks.v1_itemDataCallback = display.itemDataProc;
	callbacks.v1_itemNotificationCallback = display.itemNotificationProc;
	OS.SetDataBrowserCallbacks (handle, callbacks);
}

int itemDataProc (int browser, int id, int property, int itemData, int setValue) {
	int index = id - 1;
	if (!(0 <= index && index < items.length)) return OS.noErr;
	TreeItem item = items [index];
	switch (property) {
		case CHECK_COLUMN_ID: {
			if (setValue != 0) {
				short [] theData = new short [1];
				OS.GetDataBrowserItemDataButtonValue (itemData, theData);
				item.checked = theData [0] == OS.kThemeButtonOn;
				Event event = new Event ();
				event.item = item;
				event.detail = SWT.CHECK;
				postEvent (SWT.Selection, event);
			} else {
				short theData = (short)(item.checked ? OS.kThemeButtonOn : OS.kThemeButtonOff);
				OS.SetDataBrowserItemDataButtonValue (itemData, theData);
			}
			break;
		}
		case COLUMN_ID: {
			String text = item.text;
			char [] buffer = new char [text.length ()];
			text.getChars (0, buffer.length, buffer, 0);
			int ptr = OS.CFStringCreateWithCharacters (OS.kCFAllocatorDefault, buffer, buffer.length);
			if (ptr == 0) error (SWT.ERROR_CANNOT_SET_TEXT);
			OS.SetDataBrowserItemDataText (itemData, ptr);
			OS.CFRelease (ptr);
			break;
		}
		case OS.kDataBrowserItemIsContainerProperty: {
			for (int i=0; i<items.length; i++) {
				if (items [i] != null && items [i].parentItem == item) {
					OS.SetDataBrowserItemDataBooleanValue (itemData, true);
				}
			}
			break;
		}
	}
	return OS.noErr;
}

int itemNotificationProc (int browser, int id, int message) {
	int index = id - 1;
	if (!(0 <= index && index < items.length)) return OS.noErr;
	TreeItem item = items [index];
	switch (message) {
		case OS.kDataBrowserItemSelected:
		case OS.kDataBrowserItemDeselected: {
			if (ignoreSelect) break;
			int [] first = new int [1], last = new int [1];
			OS.GetDataBrowserSelectionAnchor (handle, first, last);
			boolean selected = false;
			if ((style & SWT.MULTI) != 0) {
				int modifiers = OS.GetCurrentEventKeyModifiers ();
				if ((modifiers & OS.shiftKey) != 0) {
					if (message == OS.kDataBrowserItemSelected) {
						selected = first [0] == id || last [0] == id;
					} else {
						selected = id == anchorFirst || id == anchorLast;
					}
				} else {
					if ((modifiers & OS.cmdKey) != 0) {
						selected = true;
					} else {
						selected = first [0] == last [0];
					}
				}
			} else {
				selected = message == OS.kDataBrowserItemSelected;
			}
			if (selected) {
				anchorFirst = first [0];
				anchorLast = last [0];
				Event event = new Event ();
				event.item = item;
				postEvent (SWT.Selection, event);
			}
			break;
		}	
		case OS.kDataBrowserItemDoubleClicked: {
			Event event = new Event ();
			event.item = item;
			postEvent (SWT.DefaultSelection, event);
			break;
		}
		case OS.kDataBrowserContainerClosed: {		
			Event event = new Event ();
			event.item = item;
			sendEvent (SWT.Collapse, event);
			break;
		}
		case OS.kDataBrowserContainerOpened: {
			Event event = new Event ();
			event.item = item;
			sendEvent (SWT.Expand, event);
			int count = 0;
			for (int i=0; i<items.length; i++) {
				if (items [i] != null && items [i].parentItem == item) count++;
			}
			int [] ids = new int [count];
			for (int i=0; i<items.length; i++) {
				if (items [i] != null && items [i].parentItem == item) {
					ids [items [i].index] = items [i].id;
				}
			}
			OS.AddDataBrowserItems (handle, id, ids.length, ids, 0);
			//BUG - items that are added in the expand callback should draw checked but don't
//			OS.UpdateDataBrowserItems (handle, id, ids.length, ids, OS.kDataBrowserItemNoProperty, OS.kDataBrowserNoItem);
			break;
		}
	}
	return OS.noErr;
}

void releaseWidget () {
	for (int i=0; i<items.length; i++) {
		TreeItem item = items [i];
		if (item != null && !item.isDisposed ()) {
			item.releaseResources ();
		}
	}
	super.releaseWidget ();
}

public void removeAll () {
	checkWidget ();
	if (OS.RemoveDataBrowserItems (handle, OS.kDataBrowserNoItem, 0, null, 0) != OS.noErr) {
		error (SWT.ERROR_ITEM_NOT_REMOVED);
	}
	for (int i=0; i<items.length; i++) {
		TreeItem item = items [i];
		if (item != null && !item.isDisposed ()) item.releaseResources ();
	}
	items = new TreeItem [4];
}

public void removeSelectionListener (SelectionListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	eventTable.unhook (SWT.Selection, listener);
	eventTable.unhook (SWT.DefaultSelection, listener);	
}

public void removeTreeListener(TreeListener listener) {
	checkWidget ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (SWT.Expand, listener);
	eventTable.unhook (SWT.Collapse, listener);
}

public void setInsertMark (TreeItem item, boolean before) {
	checkWidget ();
	int hItem = 0;
	if (item != null) {
		if (item.isDisposed()) error(SWT.ERROR_INVALID_ARGUMENT);
	}
}

public void selectAll () {
	checkWidget ();
	if ((style & SWT.SINGLE) != 0) return;
	ignoreSelect = true;
	OS.SetDataBrowserSelectedItems (handle, 0, null, OS.kDataBrowserItemsAssign);
	ignoreSelect = false;
}

public void setSelection (TreeItem [] items) {
	checkWidget ();
	if (items == null) error (SWT.ERROR_NULL_ARGUMENT);
	int[] ids = new int [items.length];
	for (int i=0; i<items.length; i++) {
		if (items [i] == null) error (SWT.ERROR_INVALID_ARGUMENT);
		if (items [i].isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
		ids [i] = items [i].id;
		showItem (items [i], false);
	}
	ignoreSelect = true;
	OS.SetDataBrowserSelectedItems (handle, ids.length, ids, OS.kDataBrowserItemsAssign);
	ignoreSelect = false;
}

public void showItem (TreeItem item) {
	checkWidget ();
	if (item == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (item.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	showItem (item, true);
}

void showItem (TreeItem item, boolean scroll) {
	int count = 0;
	TreeItem parentItem = item.parentItem;
	while (parentItem != null && !parentItem.getExpanded ()) {
		count++;
		parentItem = parentItem.parentItem;
	}
	int index = 0;
	parentItem = item.parentItem;
	TreeItem [] path = new TreeItem [count];
	while (parentItem != null && !parentItem.getExpanded ()) {
		path [index++] = parentItem;
		parentItem = parentItem.parentItem;
	}
	for (int i=path.length-1; i>=0; --i) {
		path [i].setExpanded (true);
	}
	int options = OS.kDataBrowserRevealWithoutSelecting;
	if (scroll)  options |= OS.kDataBrowserRevealAndCenterInView;
	OS.RevealDataBrowserItem (handle, item.id, COLUMN_ID, (byte)options);
}

public void showSelection () {
	checkWidget ();
	//OPTIMIZE
	TreeItem [] selection = getSelection ();
	if (selection.length > 0) showItem (selection [0], true);
}

}

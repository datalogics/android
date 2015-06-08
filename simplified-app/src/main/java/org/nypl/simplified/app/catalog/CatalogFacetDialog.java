package org.nypl.simplified.app.catalog;

import java.util.ArrayList;

import org.nypl.simplified.app.R;
import org.nypl.simplified.books.core.FeedFacetType;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A dialog used to select facets.
 */

public final class CatalogFacetDialog extends DialogFragment implements
  OnItemClickListener
{
  private static final String GROUP_ID;
  private static final String GROUP_NAME_ID;

  static {
    GROUP_ID = "org.nypl.simplified.app.catalog.CatalogFacetDialog.facets";
    GROUP_NAME_ID =
      "org.nypl.simplified.app.catalog.CatalogFacetDialog.facets_name";
  }

  public static CatalogFacetDialog newDialog(
    final String in_facet_group_name,
    final ArrayList<FeedFacetType> in_facet_group)
  {
    NullCheck.notNull(in_facet_group);
    final CatalogFacetDialog c = new CatalogFacetDialog();
    final Bundle b = new Bundle();
    b.putString(CatalogFacetDialog.GROUP_NAME_ID, in_facet_group_name);
    b.putSerializable(CatalogFacetDialog.GROUP_ID, in_facet_group);
    c.setArguments(b);
    return c;
  }

  private @Nullable ArrayList<FeedFacetType>          group;
  private @Nullable ArrayAdapter<String>              group_adapter;
  private @Nullable String                            group_name;
  private @Nullable CatalogFacetSelectionListenerType listener;

  public CatalogFacetDialog()
  {
    // Fragments must have no-arg constructors.
  }

  @Override public void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setStyle(DialogFragment.STYLE_NORMAL, R.style.SimplifiedBookDialog);

    final Bundle b = NullCheck.notNull(this.getArguments());

    @SuppressWarnings("unchecked") final ArrayList<FeedFacetType> in_group =
      NullCheck.notNull((ArrayList<FeedFacetType>) b
        .getSerializable(CatalogFacetDialog.GROUP_ID));

    this.group = in_group;
    this.group_name =
      NullCheck.notNull(b.getString(CatalogFacetDialog.GROUP_NAME_ID));

    final ArrayList<String> in_strings =
      new ArrayList<String>(in_group.size());
    for (final FeedFacetType f : in_group) {
      in_strings.add(f.facetGetTitle());
    }

    this.group_adapter =
      new ArrayAdapter<String>(
        this.getActivity(),
        android.R.layout.simple_list_item_1,
        android.R.id.text1,
        in_strings);
  }

  @Override public View onCreateView(
    final @Nullable LayoutInflater inflater_mn,
    final @Nullable ViewGroup container,
    final @Nullable Bundle state)
  {
    final LayoutInflater inflater = NullCheck.notNull(inflater_mn);

    final ViewGroup layout =
      NullCheck.notNull((ViewGroup) inflater.inflate(
        R.layout.facet_dialog,
        container,
        false));

    final ListView in_list =
      NullCheck.notNull((ListView) layout.findViewById(R.id.facet_list));
    final TextView in_title =
      NullCheck.notNull((TextView) layout.findViewById(R.id.facet_title));

    in_list.setAdapter(this.group_adapter);
    in_list.setOnItemClickListener(this);
    in_title.setText(this.group_name + ":");

    final Dialog d = this.getDialog();
    if (d != null) {
      d.setCanceledOnTouchOutside(true);
    }
    return layout;
  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> av,
    final @Nullable View v,
    final int position,
    final long id)
  {
    final FeedFacetType f =
      NullCheck.notNull(NullCheck.notNull(this.group).get(position));
    NullCheck.notNull(this.listener).onFacetSelected(f);
  }

  @Override public void onResume()
  {
    super.onResume();

    /**
     * Force the dialog to always appear at the same size, with a decent
     * amount of empty space around it.
     */

    final Activity act = NullCheck.notNull(this.getActivity());
    final WindowManager window_manager =
      NullCheck.notNull((WindowManager) act
        .getSystemService(Context.WINDOW_SERVICE));
    final Display display =
      NullCheck.notNull(window_manager.getDefaultDisplay());

    final DisplayMetrics m = new DisplayMetrics();
    display.getMetrics(m);

    final int width = (int) (m.widthPixels * 0.75);
    final Dialog dialog = NullCheck.notNull(this.getDialog());
    final Window window = NullCheck.notNull(dialog.getWindow());
    window.setLayout(width, window.getAttributes().height);
  }

  public void setFacetSelectionListener(
    final CatalogFacetSelectionListenerType in_listener)
  {
    this.listener = NullCheck.notNull(in_listener);
  }
}
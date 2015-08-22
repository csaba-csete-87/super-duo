package it.jaschke.alexandria;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.nostra13.universalimageloader.core.ImageLoader;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ZXingScannerView.ResultHandler {

    @Bind(R.id.ean)
    EditText ean;

    @OnTextChanged(R.id.ean)
    public void onEanAfterTextChanged(Editable s) {
        String ean = s.toString();
        //catch isbn10 numbers
        if (ean.length() == 10 && !ean.startsWith("978")) {
            ean = "978" + ean;
        }
        if (ean.length() < 13) {
            clearFields();
            return;
        }

        mSearchButton.setEnabled(true);
        mSearchButton.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.scan_button)
    public void onScanButtonClicked() {
        showScanner();
    }

    @Bind(R.id.save_button)
    Button mSaveButton;

    @OnClick(R.id.save_button)
    public void onSaveButtonClicked() {
        ean.setText("");
        mSearchButton.setVisibility(View.VISIBLE);
        mSearchButton.setEnabled(false);

        Toast.makeText(getActivity(), getString(R.string.book_successfully_added), Toast.LENGTH_SHORT).show();
    }

    @Bind(R.id.delete_button)
    Button mDeleteButton;

    @OnClick(R.id.delete_button)
    public void onDeleteButtonClicked() {
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean.getText().toString());
        bookIntent.setAction(BookService.DELETE_BOOK);
        getActivity().startService(bookIntent);
        ean.setText("");
        mSearchButton.setVisibility(View.VISIBLE);
        mSearchButton.setEnabled(false);
    }

    @Bind(R.id.search_button)
    Button mSearchButton;

    @OnClick(R.id.search_button)
    public void onSearchButtonClicked() {
        //Once we have an ISBN, start a book intent
        Intent bookIntent = new Intent(getActivity(), BookService.class);
        bookIntent.putExtra(BookService.EAN, ean.getText().toString());
        bookIntent.setAction(BookService.FETCH_BOOK);
        getActivity().startService(bookIntent);
        AddBook.this.restartLoader();

        mSearchButton.setVisibility(View.GONE);
        mProgressDialog.show();
    }

    @Bind(R.id.scanner_container)
    LinearLayout mScannerContainer;

    @Bind(R.id.scanner_holder)
    FrameLayout mScannerHolder;

    @Nullable
    @Bind(R.id.start_scan_container)
    LinearLayout mStartScanContainer;

    @OnClick(R.id.cancel_scan)
    public void onCancelScanButtonClicked() {
        hideScanner();
    }

    @Bind(R.id.bookTitle)
    TextView mBookTitle;

    @Bind(R.id.bookSubTitle)
    TextView mBookSubTitle;

    @Bind(R.id.authors)
    TextView mAuthors;

    @Bind(R.id.categories)
    TextView mCategories;

    @Bind(R.id.bookCover)
    ImageView mBookCover;

    private final String EAN_CONTENT = "eanContent";

    private ZXingScannerView mScannerView;
    private ProgressDialog mProgressDialog;

    public AddBook() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ean != null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

        if (savedInstanceState != null) {
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(getString(R.string.searching));
    }

    private void showScanner() {
        mScannerContainer.setVisibility(View.VISIBLE);
        if(mStartScanContainer != null){
            mStartScanContainer.setVisibility(View.GONE);
        }
        mScannerView = new ZXingScannerView(getActivity());
        mScannerHolder.addView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    private void hideScanner() {
        mScannerContainer.setVisibility(View.GONE);
        if(mStartScanContainer != null) {
            mStartScanContainer.setVisibility(View.VISIBLE);
        }
        mScannerView.stopCamera();
        mScannerHolder.removeAllViews();
        mScannerView = null;
    }

    private void restartLoader() {
        int LOADER_ID = 1;
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (ean.getText().length() == 0) {
            return null;
        }
        String eanStr = ean.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
        }
        Log.e("eanStr", "" + eanStr);
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        mProgressDialog.hide();
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        mBookTitle.setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        mBookSubTitle.setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if (authors != null && !TextUtils.isEmpty(authors)) { // NullPointer Exception fix
            String[] authorsArr = authors.split(",");
            mAuthors.setLines(authorsArr.length);
            mAuthors.setText(authors.replace(",", "\n"));
        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            ImageLoader.getInstance().displayImage(imgUrl, mBookCover);
            mBookCover.setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        mCategories.setText(categories);

        mSaveButton.setVisibility(View.VISIBLE);
        mDeleteButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void handleResult(Result result) {
        hideScanner();
        ean.setText("" + result.getText());
    }

    private void clearFields() {
        mBookTitle.setText("");
        mBookSubTitle.setText("");
        mAuthors.setText("");
        mCategories.setText("");
        mBookCover.setVisibility(View.INVISIBLE);
        mSaveButton.setVisibility(View.INVISIBLE);
        mDeleteButton.setVisibility(View.INVISIBLE);
    }
}

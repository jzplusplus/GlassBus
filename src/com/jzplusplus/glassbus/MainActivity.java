package com.jzplusplus.glassbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;

public class MainActivity extends Activity implements LocationListener{
	private final String TAG = "GlassBus";
	
	private LocationManager locationManager;
	
	private List<View> cards;
    private CardScrollView busCardScrollView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		cards = new ArrayList<View>();
		busCardScrollView = new CardScrollView(this);
		BusCardScrollAdapter adapter = new BusCardScrollAdapter();
        busCardScrollView.setAdapter(adapter);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		
		Log.d(TAG, "Providers: " + locationManager.getAllProviders());
		
		String provider = "network";
		
		locationManager.requestSingleUpdate(provider, this, null);
	}
	

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}



	private class FetchDataTask extends AsyncTask<String, Void, Document>
	{

		@Override
		protected Document doInBackground(String... params) {
			
			try {
				Log.d(TAG, "Getting data from: " + params[0]);
				Document doc = Jsoup.connect(params[0]).get();
				Log.d(TAG, "Got data");
				return doc;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Document result) {
			if(result == null)
				Toast.makeText(MainActivity.this, "Connection to NextBus failed", Toast.LENGTH_SHORT).show();
			else
			{
				Log.d(TAG, "Parsing data...");
				Elements elements = result.select(".inBetweenRouteSpacer");
				for(Element e: elements)
				{
					Element e2 = e.nextElementSibling();
					while(e2.className().contains("link"))
					{
						String route = e.child(0).html();
						String direction = e2.select(".directionName").get(0).html();
						String stop = e2.child(0).childNode(3).toString();
						String times = e2.select(".preds").html();
						//Log.d(TAG, "Route: " + route + "\n\n" + "Dir: " + direction + "\n\n" + "Stop: " + stop + "\n\n" + "Times: " + times);
						
						String cardFooter = "Route: " + route + "\n" + direction + "\n" + stop;
						cardFooter = cardFooter.replace("&amp;", "&");
						
						String cardText = times.replace("&amp;", "&");
											
						LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				        View view = layoutInflater.inflate(R.layout.bus_card, null);
				        ((TextView)view.findViewById(R.id.time)).setText(cardText);
				        ((TextView)view.findViewById(R.id.otherdata)).setText(cardFooter);
				        
				        cards.add(view);
				        
				        if(e2.nextElementSibling() == null) break;
				        e2 = e2.nextElementSibling();
					}
				}
				
				if(cards.size() == 0)
				{
					Card c = new Card(MainActivity.this);
					c.setText("No routes found");
					cards.add(c.toView());
				}
				
				busCardScrollView.activate();
		        setContentView(busCardScrollView);
			}
		}
		
	}
	
	private class BusCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int findIdPosition(Object id) {
            return -1;
        }

        @Override
        public int findItemPosition(Object item) {
            return cards.indexOf(item);
        }

        @Override
        public int getCount() {
            return cards.size();
        }

        @Override
        public Object getItem(int position) {
            return cards.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return cards.get(position);
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		Log.d(TAG, "Location Age: " + (System.currentTimeMillis() - location.getTime()));
		
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		
		String url = String.format(Locale.US, "http://www.nextbus.com/webkit/predsByLoc.jsp?lat=%.15f&lon=%.15f", lat, lon);
		
		(new FetchDataTask()).execute(url);
	}


	@Override
	public void onProviderDisabled(String arg0) {}
	@Override
	public void onProviderEnabled(String arg0) {}
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
}

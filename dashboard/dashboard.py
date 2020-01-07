import data_manager
import dash
import dash_core_components as dcc
import dash_html_components as html
import dash_table_experiments as dt
import firebase_manager
import json
import pandas as pd
import plotly.graph_objs as go
from dash.dependencies import Input, Output
dm = data_manager.DataManager()

user = []

with open("users.csv") as users:
    for line in users.readlines():
        u = firebase_manager.append_user_to_appid(line.strip())
        user.append(u)

app = dash.Dash()
app.scripts.config.serve_locally = True
app.title = 'Monitoring'


app.layout = html.Div([
    # represents the URL bar, doesn't render anything
    dcc.Dropdown(
        id='my-dropdown',
        value="",
        options=[{"label": usr, "value": usr} for usr in user]
    ),

    # content will be rendered in this element
    html.Div([
        dt.DataTable(
            rows=[{}],  # initialise the rows
            row_selectable=True,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable')],
        id='page-content')
])


def gen(selected):
    queries = dm.sync_data(selected)
    queries["AppUsages"] = queries["AppUsages"].astype(str)
    queries["UsageEvents"] = queries["UsageEvents"].astype(str)

    queries = queries.reset_index()

    act = dataframe_or_null(dm._load_activities_data(selected)).reset_index()


    location = dataframe_or_null(dm._load_location_data(selected)).reset_index()
    app_usage = dm._load_app_usage_data(selected).reset_index()


    link_res = dataframe_or_null(dm._load_relevant_result_data(selected)).reset_index()
    history = dataframe_or_null(dm._load_history_data(selected)).reset_index()


    try:
        #act_colors = act.event.replace(regex={r'^START_.*':'rgba(139, 195, 74,1)', '^STOP_.*':'rgba(244, 67, 54,1)'})
        act_symbols = act.event.replace(regex={r'.*_DRIVING':'rgba(63, 81, 181,1.0)', 
        '.*_STILL.*':'rgba(244, 67, 54,1.0)',
        '.*_WALKING.*':'rgba(76, 175, 80,1.0)',
        '.*_RUNNING.*':'rgba(255, 152, 0,1.0)',
        '.*_CYCLING':'rgba(121, 85, 72,1.0)'})
    except:
        pass 

    print("selected {}".format(selected))

    return html.Div([
        html.H4('{} DataTable'.format(selected)),
        dt.DataTable(
            rows=queries.to_dict('records'),
            columns=queries.columns,
            row_selectable=False,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable-queries'
        ),
        html.H3("relevant results"),
        dt.DataTable(
            rows=link_res.sort_values('timestamp', ascending = False).head().to_dict('records'),

            # optional - sets the order of columns
            columns=link_res.columns,

            row_selectable=False,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable-link_res'
        ),
        html.Div(id='selected-indexes'),
        dcc.Graph(
            id='graph-activities',
            figure=go.Figure(data=[
                go.Scatter(
                    x=act.timestamp ,
                    y=act.timestamp.apply(lambda y: 10),
                    mode='markers',
                    text=act.event,
                    marker=dict(
                        color=act_symbols.tolist(),
                        size = 10,
                        symbol=8,
                    )
                    # orientation = 'h',
                )]
            ),
        ),

        dt.DataTable(
            rows=act.to_dict('records'),

            # optional - sets the order of columns
            columns=act.columns,

            row_selectable=False,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable-activities'
        ),
        html.H3("location"),
        dt.DataTable(
            rows=location.head().to_dict('records'),

            # optional - sets the order of columns
            columns=location.columns,

            row_selectable=False,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable-location'
        ),
        html.H3("app usage"),
        dt.DataTable(
            rows=app_usage.sort_values('timestamp', ascending = False).head().to_dict('records'),

            # optional - sets the order of columns
            columns=app_usage.columns,

            row_selectable=False,
            filterable=True,
            sortable=True,
            selected_row_indices=[],
            id='datatable-app'
        ),
        
        #"history",
        #dt.DataTable(
        #    rows=history.head().to_dict('records'),

            # optional - sets the order of columns
        #    columns=history.columns,

        #    row_selectable=False,
        #    filterable=True,
        #    sortable=True,
        #    selected_row_indices=[],
        #    id='datatable-history'
        #)        

    ], className="container", id="container")


@app.callback(Output('page-content', 'children'), [Input('my-dropdown', 'value')])
def update_graph(selected_dropdown_value):
    if selected_dropdown_value is not "":
        print(selected_dropdown_value)
        return gen(selected_dropdown_value)
    else:
        return gen(user[0])


def dataframe_or_null(df):
    if df is not None:
        if not df.empty:
            return df
    else:
        return pd.DataFrame(data={'timestamp': [], "event": []})


if __name__ == '__main__':
    app.run_server(debug=True)

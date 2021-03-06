<%
    // although called "patientDashboard" this is actually the patient visits screen, and clinicianfacing/patient is the main patient dashboard

    ui.decorateWith("appui", "standardEmrPage")

	ui.includeCss("coreapps", "patientdashboard/patientDashboard.css")

    ui.includeJavascript("uicommons", "bootstrap-collapse.js")
    ui.includeJavascript("uicommons", "bootstrap-transition.js")


    def tabs = [
        [ id: "visits", label: ui.message("coreapps.patientDashBoard.visits"), provider: "coreapps", fragment: "patientdashboard/visits" ],
        patientTabs.collect{
            [id: it.id, label: ui.message(it.label), provider: it.extensionParams.provider, fragment: it.extensionParams.fragment]
        }
    ]

    tabs = tabs.flatten()

%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.escapeJs(ui.encodeHtmlContent(ui.format(patient.patient))) }" ,
            link: '${ ui.urlBind("/" + contextPath + dashboardUrl, [ patientId: patient.patient.id ] ) }'}
    ];

    jq(function(){
        jq(".tabs").tabs();

        // make sure we reload the page if the location is changes; this custom event is emitted by by the location selector in the header
        jq(document).on('sessionLocationChanged', function() {
            window.location.reload();
        });
    });

    var patient = { id: ${ patient.id } };
    var encounterCount = ${ encounterCount };    // This variable will be reused in msfvisits.gsp
</script>
<% if (includeFragments) {

    includeFragments.each {
        // create a base map from the fragmentConfig if it exists, otherwise just create an empty map
        def configs = [:];
        if(it.extensionParams.fragmentConfig != null){
            configs = it.extensionParams.fragmentConfig;
        }

        configs.patient = patient;   // add the patient to the map %>
        ${ui.includeFragment(it.extensionParams.provider, it.extensionParams.fragment, configs)}
    <%}
} %>

${ ui.includeFragment("coreapps", "patientHeader", [ patient: patient.patient, activeVisit: activeVisit, appContextModel: appContextModel ]) }
<div class="actions dropdown">
    <span class="dropdown-name"><i class="icon-cog"></i>${ ui.message("coreapps.actions") }<i class="icon-sort-down"></i></span>
    <ul>
        <% overallActions.each {
            def url = it.url
            url = it.url(contextPath, appContextModel, ui.thisUrl())
        %>
            <li>
                <a href="${ url }"><i class="${ it.icon }"></i>${ ui.message(it.label) }</a>
            </li>
        <% } %>
    </ul>
</div>

<div class="tabs" xmlns="http://www.w3.org/1999/html">
    <div class="dashboard-container">

        <ul>
            <% tabs.each { %>
            <li>
                <a href="#${ it.id }">
                    ${ it.label }
                </a>
            </li>
            <% } %>

        </ul>

        <% tabs.each { %>
        <div id="${it.id}">
            ${ ui.includeFragment(it.provider, it.fragment, [ patient: patient ]) }
        </div>
        <% } %>

    </div>
</div>
<script type="text/javascript">
    jq(document).ajaxStop(function() {
        jq('<ul id="encounterTypeList"></ul>').insertBefore('#encountersList');
        // move encounters with a similar encounter type to the same list
        jq('#encountersList li.encounter').each(function(){
            var encounter_type = jq(this).attr('data-encounter-type-uuid');
            // check if this element exists within the encounterTypeList
            if (!jq('#' + encounter_type + '-li').length) {
                // create a new element for the encounter
                jq('#encounterTypeList').append('<li id="'+ encounter_type + '-li">' +
                    '<ul id="'+ encounter_type + '-ul" class="encounter-list">' +
                        '<li class="encounter-summary" onclick="toggle()">' + '' +
                            '<h4 class="encounter-name">' + jq(this).attr('data-encounter-type-name') + '</h4>' +
                            '<div class="encounter-date"> <i class="icon-time"></i> <strong>' + jq(this).attr('data-encounter-time') + ' </strong>' + jq(this).attr('data-encounter-date') + ' </div>' +
                            '<p>${ ui.message("coreapps.by") } <strong class="provider">' +jq(this).attr('data-encounter-provider') + ' </strong> ' +
                                '${ ui.message("coreapps.in") } <strong class="location">' +jq(this).attr('data-encounter-location') +' </strong></p> '+
                        '</li>'+
                    '</ul>' +
                    '</li>');
            }
            jq('#' + encounter_type + '-ul').append(jq(this));
        });
    });
</script>

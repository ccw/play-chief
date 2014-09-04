var margin = {top: 20, right: 150, bottom: 50, left: 60},
    width = 1250 - margin.left - margin.right,
    h = 225;

var fd = d3.time.format("%m/%d/%Y %H:%M");
var ft = d3.format(".2s");
var tn = function (d) { return d.y; };
var td = function (d) { return fd.parse(d.x); };

var x = d3.time.scale().range([0, width]);
var y = d3.scale.linear();
var z = d3.scale.linear();
var l = d3.svg.line()
              .interpolate("monotone")
              .x(function (d) { return x(td(d)); })
              .y(function (d) { return d.type === 'count' ? z(d.y) : y(tn(d)); });
var r = d3.scale.category10();

var x_transfer = function(d) { return x(td(d)); };
var y_transfer = function(d) { return d.type === 'count' ? z(tn(d)) : y(tn(d)); };
var l_transfer = function (d) { return d ? l(d.values) : 0; };
var h_transfer = function(ds, d) { return {values: _.where(ds, {x: d.x}).sort(function(a, b) { return a.y - b.y; } )};};
var l_stroke = function (d) { return r(d.key); };

var x_comparator = function (a, b) { return td(a) - td(b); };

var tip = d3.tip()
            .attr('class', 'd3-tip')
            .offset([-10, 0])
            .html(function (d) {
                if (d && d.length > 0) {
                    return "<div class='ui raised segment'>" +
                           "<div class='ui black ribbon label'>" + d[0].x + "</div>" +
                           "<div class='ui rows'>" +
                           _.reduce(d, function(m, v) {
                               return m + "<div class='item'>" +
                                      "<span class='ui horizontal label'>" + v.type.toUpperCase() + ":</span> " +
                                      "<span class='ui label circular measure' style='background-color: " + r(v.type) + "'>" + ft(v.y) + "</span></div>"}, "")
                           + "</div></div>";
                } else
                    return "";
            });

var xAxis;

var uri;

var cache;

var draw = function (options) {
    cache = {};
    uri = options['fake'] ? '/chief/fake?' : '/chief/graph?';
    uri += d3.map(options).entries().map(function(d) {
        if (typeof d.value === 'string') {
            return d.key + "=" + d.value;
        } else {
            return _.map(d.value, function(v) { return d.key + "=" + v; }).join('&');
        }
    }).join('&');

    $('#chart svg').remove();
    d3.json(uri, function (error, data) {
        $('#spinner').spin(false);
        if (!data || data.length == 0) { return; }

        data.sort(x_comparator);
        var groups = d3.nest().key(function(d) { return d.action; })
                              .key(function(d) { return d.type; })
                              .entries(data)
                              .sort(function(a, b) { return a.key > b.key; });
        var height = h * groups.length + (20 * groups.length - 1);

        var s = d3.select("#chart").append("svg")
                  .attr("width", width + margin.left + margin.right)
                  .attr("height", height + margin.top + margin.bottom)
                  .append("g")
                  .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        s.call(tip);

        cache['data'] = data;

        _draw(s, data, groups);

        setInterval(_update, 60000);
    });
};

var _refreshData = function(origin, updated, loader, limit) {
    _.filter(origin, function(d) { return loader(d) < limit; }).forEach(function(d) {
        origin.splice(origin.indexOf(d), 1);
    });
    _.filter(updated, function(d) { return loader(d) > loader(origin[origin.length - 1]); }).forEach(function(d) {
        origin.push(d);
    });
};

var _update = function() {
    d3.json(uri, function (error, source) {
        if (!source || source.length == 0) { return; }

        var data = source.sort(x_comparator);
        var latest = data[data.length - 1];
        var boundary = td(latest);
        var base = boundary - 50 * cache['time_step'];
        _refreshData(cache['data'], data, td, base);
        x.domain([base, boundary]);
        y.range([h - 25, 0]).domain([0, d3.max(cache['data'], tn)]);
        z.range([h, h - 20]).domain([0, d3.max(cache['data'], function(d) { return d.type === 'count' ? d.y : 0; }) + 10]);

        d3.nest().key(function(d) { return d.action; })
                 .key(function(d) { return d.type; })
                 .entries(data)
                 .sort(function(a, b) { return a.key > b.key; }).forEach(function(group) {

            var id = group.key.replace('/', '');
            var options = cache[id];
            if (options) {
                _.each(group.values, function(element) {
                    _refreshData(_.find(options.elements, function(ce) { return ce.key == element.key; }).values,
                                 element.values,
                                 td, base);
                });
                options.points = _.reduce(options.elements, function (m, v) { return m.concat(v.values); }, []).sort(x_comparator);
                options.transform = true;
                refreshChart(options);
            }        
        });
    });
};

var _draw = function(s, data, groups) {
    r.domain(groups[0].values.map(function(d) { return d.key; }).sort());

    var latest = data[data.length - 1];
    var previous = _.find(data.reverse(), function(d) { return d.x != latest.x; });
    var boundary = td(latest);
    cache['time_step'] = boundary - td(previous);
    x.domain([boundary - 50 * cache['time_step'], boundary]);
    cache['x_step'] = (x(boundary) - x(td(previous)));
    y.range([h - 25, 0]).domain([0, d3.max(data, tn)]);
    z.range([h, h - 20]).domain([0, d3.max(data, function(d) { return d.type === 'count' ? d.y : 0; }) + 10]);

    xAxis = s.append("g")
             .attr("class", "x axis")
             .call(d3.svg.axis()
                         .scale(x)
                         .orient("top")
                         .tickFormat(d3.time.format('%H:%M')));

    for (var i = 0; i < groups.length; i ++) {
        var options = {};
        options.id = groups[i].key.replace('/', '');
        options.title = [groups[i].key];
        options.elements = groups[i].values;
        options.points = _.reduce(options.elements, function (m, v) { return m.concat(v.values); }, []).sort(x_comparator);
        options.canvas = s.append("g")
                          .attr('class', options.id.toLowerCase())
                          .attr("transform", "translate(" + 0 + "," + (this.margin.top + (h * i) + (20 * i)) + ")");
        chart.call(this, options);
        cache[options.id] = options;
    }
};

var chart = function(options) {
    var canvas = options.canvas;

    // ruler
    canvas.append('g')
          .attr("clip-path", "url(#clip)")
          .append("path")
          .data([h_transfer(options.points, options.points[0])])
          .attr("class", "rule")
          .attr("d", l_transfer)
          .style("stroke", "gray")
          .style("stroke-width", 2)
          .style("stroke-dasharray", "5, 5")
          .style("opacity", 0);

    // y axis
    options.yAxis = canvas.append("g")
        .attr("class", "y axis")
        .attr("transform", "translate(-10,5)")
        .call(d3.svg.axis()
                    .scale(y)
                    .orient("left")
                    .ticks(5)
                    .tickFormat(ft))
        .append("text")
        .attr("transform", "rotate(-90)")
        .attr("y", 6)
        .attr("dy", ".3em")
        .style("text-anchor", "end")
        .attr("font-size", "0.8em")
        .text("ms");

    // counter area
    canvas.append('svg:rect')
        .attr('x', -5)
        .attr('y', h - 15)
        .attr('width', width + 10)
        .attr('height', 25)
        .style('fill', 'rgba(125, 125, 125, 0.3)');

    // line
    options.line = canvas.selectAll(".action")
                         .data(options.elements)
                         .enter()
                         .append("g")
                         .attr("class", "action-" + options.id)
                         .attr("clip-path", "url(#clip)")
                         .append("path")
                         .attr("class", "line")
                         .attr("d", l_transfer)
                         .style("stroke", l_stroke);

    // chart title
    var title = canvas.selectAll(".title")
                      .data(options.title)
                      .enter().append("g")
                      .attr("class", "title")
                      .attr("transform", "translate(0,10)");
    title.append("text")
         .attr("x", 15)
         .attr("y", -10)
         .attr("dy", ".35em")
         .style("text-anchor", "begin")
         .text(options.id);

    // legend
    var legend = canvas.selectAll(".legend")
                       .data(r.domain())
                       .enter().append("g")
                       .attr("class", "legend")
                       .attr("transform", function (d, i) {
                           return "translate(0," + (12 * i) + ")";
                       });
    legend.append("rect")
          .attr("x", width + 5)
          .attr("y", 0)
          .attr("width", 10)
          .attr("height", 10)
          .style("fill", function (d) {
              return r(d);
          });
    legend.append("text")
          .attr("x", width + 17)
          .attr("y", 5)
          .attr("dy", ".35em")
          .style("text-anchor", "begin")
          .attr("font-size", "0.8em")
          .text(function (d) {
              return d;
          });

    refreshChart(options);
};

var refreshChart = function(options) {
    var canvas = options.canvas;

    options.line.attr("d", l_transfer)
                .attr("transform", null)
                .style("stroke", l_stroke);
    options.line.transition()
                .duration(500)
                .attr("d", l_transfer)
                .attr("transform",
                    options.transform ? "translate(" + (-cache['x_step']) + ")" : null
                )
                .ease("linear");

    //points
    canvas.selectAll(".circle").remove();
    var cycle = canvas.selectAll(".circle")
                      .data(options.points);
    cycle.enter()
         .append("svg:circle")
         .attr("class", "circle")
         .attr("r", 3)
         .attr("cx", x_transfer)
         .attr("cy", y_transfer)
         .style("stroke", function (d) {
             return r(d.type);
         })
         .style('fill', '#FFF')
         .on('mouseover', function (d) {
             var rule = h_transfer(options.points, d);
             tip.show(rule.values.sort(function(a, b) { return a.type > b.type; }));
             canvas.select(".rule")
                   .data([rule])
                   .attr("transform",
                       options.transform ? "translate(" + (-cache['x_step']) + ")" : null
                   )
                   .attr('d', l_transfer)
                   .transition()
                   .duration(100)
                   .style('opacity', 1);
         })
         .on('mouseout', function(d) {
             tip.hide();
             canvas.select(".rule")
                   .style('opacity', 0);
         });
    cycle.transition()
         .duration(500)
         .attr("cx", x_transfer)
         .attr("cy", y_transfer)
         .attr("transform",
             options.transform ? "translate(" + (-cache['x_step']) + ")" : null
         )
         .ease("linear");

    options.yAxis.transition()
                 .duration(500)
                 .ease("linear")
                 .call(d3.svg.axis()
                             .scale(y)
                             .orient("left")
                             .ticks(5)
                             .tickFormat(ft));

    xAxis.transition()
         .duration(500)
         .ease("linear")
         .call(d3.svg.axis()
                     .scale(x)
                     .orient("top")
                     .tickFormat(d3.time.format('%H:%M')));
};

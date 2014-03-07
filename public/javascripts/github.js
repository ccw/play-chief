$(document).ready(function() {
    $('.ui.sidebar').first().sidebar('attach events', '#call-button');
    $('#execute-button').on('click', loadChanges);
});

function loadChanges() {
    $("#tables").html("");
    $('#spinner').spin({length:50, radius: 30, width: 15, top: 200});

    searchCommits();
//      .subscribe(function(commit) {
//        console.log("next");
//        //console.log(commit);
//        loadHTMLTables(commit, tables);
//      }, function(e) {
//        console.log("error");
//        //console.log(e);
//        $('<div></div>').addClass("column" ).text(e).appendTo(tables);
//        $('#spinner').spin(false);
//      }, function() {
//        console.log("complete");
//        $('#spinner').spin(false);
//      });
}

var marker = -1;
function searchCommits () {
    marker = -1;
    $.ajax({
        url: '/github/commits/' + $('input[name="owner"]').val() + '/' + $('input[name="repo"]').val() + '?since=' + $('input[name="since"]').val(),
        dataType: 'text',
        xhrFields: {
            onreadystatechange: function(e) {
                var t = e.target.response;
                if (4 == e.target.readyState) {
                    $('#spinner').spin(false);
                    console.log("onreadystatechange : completed");
                    var loadedText = t.substring(marker >= 0 ? marker : 0);
                    try {
                        loadHTMLTables(JSON.parse(loadedText), $("#tables"));
                    } catch (e) {
                        console.log(e);
                        console.log("Fail to parse loaded text");
                        console.log(loadedText);
                    }
                }
            },
            onprogress: function (e) {
                var t = e.target.response;
                if (4 != e.target.readyState) {
                    var m = t.lastIndexOf("{\"meta");
                    console.log("marker => " + marker + ", m => " + m);
                    if (m >= 0) {
                        if (marker !== m && marker >= 0) {
                            var txt = t.substring(marker, m);
                            try {
                                loadHTMLTables(JSON.parse(txt), $("#tables"));
                            } catch (e) {
                                console.log(e);
                                console.log("Fail to parse loaded text");
                                console.log(txt);
                            }
                            marker = m;
                        } else if (marker < 0) {
                            marker = m;
                        }
                    }
                }
            }
        }
    });
}

function loadHTMLTables(commit, tables) {
    var files;
    var keywords = $('textarea[name="keywords"]').val().toUpperCase().split('\n');
    if (keywords && keywords.length > 0) {
        files = _.filter(commit.files, function(file){ return _.find(keywords, function(word) { return file.full_path.toUpperCase().indexOf(word) >= 0; }); });
    } else {
        files = commit.files;
    }
    if (files.length === 0) return;

    var attrs = ["committer", "commit_date"];

    var t = $("<table></table>").addClass("ui table segment" );
    var h = $("<tr></tr>");
    var r = $("<tr></tr>");
    _.forEach(attrs, function(attr) {
        h.append($("<th></th>" ).text(attr.toUpperCase()));
        r.append($("<td></td>").css("vertical-align", "top").text(commit.meta[attr]));
    });
    var paths = _.reduce(files, function(paths, file) {
        if(!_.contains(paths, file.path)) paths.push(file.path);
        return paths;
    }, []).sort();
    files = _.groupBy(files, 'path');

    var d = $("<div></div>").addClass('ui threaded comments');
    _.forEach(paths, function(path) {
        var idx = path.indexOf('/');
        var x = $("<div></div>").addClass('comment').append(
            $("<div></div>").addClass('avatar').append($('<i></i>').css('font-size', '3em').addClass('folder outline icon')),
            $("<div></div>").addClass('content').append(
                $('<div></div>').addClass('author').css('font-weight', 'bold').text(path.substring(0, idx)),
                $('<div></div>').addClass('text').css('font-size', '0.8em').text(path.substring(idx + 1))));
        var i = $("<div></div>").addClass('comments');
        _.forEach(files[path], function(file) {
            i.append($("<div></div>").addClass('comment').append(
                $("<div></div>").addClass('content').append(
                    $("<div></div>").addClass('avatar').append($('<i></i>').css('font-size', '1.2em').addClass('file outline icon')),
                    $("<div></div>").addClass('text').css('font-size', '0.9em').text(file.file))));
        });
        x.append(i);
        d.append(x);
    });
    var b = $("<tbody></tbody>");
    b.append(r);
    var comment = commit.meta.message.split('\n').join('<br/>');
    b.append($('<tr></tr>').append($('<td></td>').attr('colspan', '2' ).css('font-size', '0.8em').html(comment)));
    var container = $("<div></div>").addClass("ui segment" ).append($("<div></div>" ).addClass("ui ribbon black label" ).text('SHA: ' + commit.meta.sha.substring(0, 10) + " Branch:" + commit.meta.branch));
    container.append(t.append($("<thead></thead>" ).append(h) ).append(b));
    container.append(d);
    tables.append($("<div></div>").addClass("column").append(container).append($("<p/>")));
}
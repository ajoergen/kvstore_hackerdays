jQuery ($) ->

  textArea = $('textarea#message')
  output = $('#output')
  ws = new WebSocket("ws://localhost:9000/segment")

  ws.onopen = () ->
    console.log "Websocket connections established"

  ws.onmessage = (messageEvent) ->
    output.html("")
    output.append messageEvent.data

  textArea.on 'input',() ->
    ws.send textArea.val()

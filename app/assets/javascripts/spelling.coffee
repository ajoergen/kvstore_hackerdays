jQuery ($) ->

  textArea = $('textarea#message2')
  output = $('#output2')
  ws2 = new WebSocket("ws://localhost:9000/spellcheck")

  ws2.onopen = () ->
    console.log "Websocket connections established"

  ws2.onmessage = (messageEvent) ->
    output.html("")
    console.log "Spelling websocket received: " + messageEvent.data
    output.append messageEvent.data

  textArea.on 'input',() ->
    ws2.send textArea.val()

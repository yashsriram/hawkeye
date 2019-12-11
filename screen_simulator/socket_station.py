class SocketStation:
    """
    Wrapper class for python sockets
    Similar class is implemented in client
    Together they block abstraction from TCP segments

    i.e. When send(data) is called at server,
        the data is padded with headers
        so that it can be extracted as is when the client calls receive()

        and vice versa

    Ex: data        = hello\n
        send(data) is called at server
        receive() is called at client
        client gets hello\n irrespective of what else exists in client receive buffer
    """
    # Must be same for client and server
    # Cannot be a digit
    # Must not be an empty string
    # Cannot be empty string or char
    head_body_delimiter = '\n'

    # Buffer_size in powers of 2 is better
    r_buffer_size = 256

    # Must be same for client and server
    # Max number of digits in length of body received
    r_body_limit = 10

    def __init__(self, sock):
        """
        :param sock: the pointer to python socket instance
        """
        self.sock = sock

    def send(self, data):
        """
        :param data: string to be sent
        :return: True if all the data is sent successfully
        padded-data = data_length + (delimiter) + data + \n
        """
        # For java's read line purposes
        data += '\n'
        data_len = len(data)
        packet = str(data_len) + SocketStation.head_body_delimiter + data
        status = self.sock.sendall(packet)

        if status is None:
            return True
        elif status is not None:
            return False

    def receive(self):
        """
        :return: the string received if successfully received
                    None otherwise
        expected format = body_length + (delimiter) + body
        """
        # Handles (nothing)(delimiter) case (not req if format is proper)
        body_len_string = '0'
        no_digits = 0
        # Finds the body length
        while True:
            letter = self.sock.recv(1).decode('utf-8')
            if letter == SocketStation.head_body_delimiter:
                break
            body_len_string += letter
            # Limits body size
            no_digits += 1
            if no_digits > SocketStation.r_body_limit:
                return None

        body_len = int(body_len_string)
        # Extracts the body
        body = ''
        while body_len > 0:
            body += self.sock.recv(min(SocketStation.r_buffer_size, body_len)).decode('utf-8')
            body_len -= SocketStation.r_buffer_size

        return body
